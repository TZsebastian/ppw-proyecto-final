package ec.edu.ups.icc.academic_events.security.services;

import ec.edu.ups.icc.academic_events.security.config.JwtProperties;
import ec.edu.ups.icc.academic_events.security.dtos.AuthResponseDTO;
import ec.edu.ups.icc.academic_events.security.dtos.CurrentUserResponseDTO;
import ec.edu.ups.icc.academic_events.security.dtos.LoginRequestDTO;
import ec.edu.ups.icc.academic_events.security.dtos.RefreshTokenRequestDTO;
import ec.edu.ups.icc.academic_events.security.dtos.RegisterRequestDTO;
import ec.edu.ups.icc.academic_events.security.entities.RefreshTokenEntity;
import ec.edu.ups.icc.academic_events.security.entities.RoleEntity;
import ec.edu.ups.icc.academic_events.security.repositories.RefreshTokenRepository;
import ec.edu.ups.icc.academic_events.security.repositories.RoleRepository;
import ec.edu.ups.icc.academic_events.security.utils.JwtUtil;
import ec.edu.ups.icc.academic_events.users.entities.UserEntity;
import ec.edu.ups.icc.academic_events.users.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

        private static final String ACTIVE_STATUS = "ACTIVE";
        private static final String PARTICIPANT_ROLE = "PARTICIPANT";

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final JwtProperties jwtProperties;
        private final RateLimitService rateLimitService;

        public AuthService(
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtUtil jwtUtil,
                        JwtProperties jwtProperties,
                        RateLimitService rateLimitService) {
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.refreshTokenRepository = refreshTokenRepository;
                this.passwordEncoder = passwordEncoder;
                this.authenticationManager = authenticationManager;
                this.jwtUtil = jwtUtil;
                this.jwtProperties = jwtProperties;
                this.rateLimitService = rateLimitService;
        }

        @Transactional
        public AuthResponseDTO register(
                        RegisterRequestDTO request,
                        String clientIp) {
                String normalizedEmail = normalizeEmail(request.email());

                rateLimitService.checkRegisterLimit(clientIp);

                if (userRepository.existsByEmail(normalizedEmail)) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "No se pudo completar el registro");
                }

                RoleEntity participantRole = roleRepository
                                .findByName(PARTICIPANT_ROLE)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "El rol PARTICIPANT no está configurado"));

                LocalDateTime now = LocalDateTime.now();

                UserEntity user = new UserEntity();
                user.setFirstName(request.firstName().trim());
                user.setLastName(request.lastName().trim());
                user.setEmail(normalizedEmail);
                user.setPasswordHash(passwordEncoder.encode(request.password()));
                user.setStatus(ACTIVE_STATUS);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                user.getRoles().add(participantRole);

                UserEntity savedUser = userRepository.save(user);

                return createTokenResponse(savedUser, clientIp);
        }

        @Transactional
        public AuthResponseDTO login(
                        LoginRequestDTO request,
                        String clientIp) {
                String normalizedEmail = normalizeEmail(request.email());

                rateLimitService.checkLoginLimit(
                                clientIp,
                                normalizedEmail);

                Authentication authentication;

                try {
                        authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        normalizedEmail,
                                                        request.password()));
                } catch (BadCredentialsException exception) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Credenciales inválidas");
                }

                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                UserEntity user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Credenciales inválidas"));

                revokeActiveTokens(user.getId());

                return createTokenResponse(user, clientIp);
        }

        @Transactional
        public AuthResponseDTO refresh(
                        RefreshTokenRequestDTO request,
                        String clientIp) {
                String rawRefreshToken = request.refreshToken();

                if (!jwtUtil.validateRefreshToken(rawRefreshToken)) {
                        throw invalidRefreshToken();
                }

                UUID tokenId;

                try {
                        tokenId = jwtUtil.getTokenId(rawRefreshToken);
                } catch (RuntimeException exception) {
                        throw invalidRefreshToken();
                }

                if (tokenId == null) {
                        throw invalidRefreshToken();
                }

                RefreshTokenEntity storedToken = refreshTokenRepository
                                .findByTokenId(tokenId)
                                .orElseThrow(this::invalidRefreshToken);

                String receivedHash = jwtUtil.hashToken(rawRefreshToken);

                if (!storedToken.getTokenHash().equals(receivedHash)
                                || storedToken.isRevoked()
                                || storedToken.isExpired()) {
                        throw invalidRefreshToken();
                }

                UserEntity user = storedToken.getUser();

                if (!ACTIVE_STATUS.equals(user.getStatus())) {
                        throw invalidRefreshToken();
                }

                UUID newTokenId = UUID.randomUUID();

                UserDetailsImpl userDetails = UserDetailsImpl.build(user);

                String newAccessToken = jwtUtil.generateAccessToken(userDetails);

                String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, newTokenId);

                LocalDateTime now = LocalDateTime.now();

                storedToken.setRevokedAt(now);
                storedToken.setReplacedByTokenId(newTokenId);
                refreshTokenRepository.save(storedToken);

                RefreshTokenEntity replacementToken = buildRefreshTokenEntity(
                                user,
                                newTokenId,
                                newRefreshToken,
                                clientIp);

                refreshTokenRepository.save(replacementToken);

                return new AuthResponseDTO(
                                newAccessToken,
                                newRefreshToken,
                                "Bearer",
                                jwtProperties.getAccessExpiration() / 1000,
                                CurrentUserResponseDTO.fromEntity(user));
        }

        @Transactional
        public void logout(RefreshTokenRequestDTO request) {
                String rawRefreshToken = request.refreshToken();
                String tokenHash = jwtUtil.hashToken(rawRefreshToken);

                refreshTokenRepository.findByTokenHash(tokenHash)
                                .filter(token -> !token.isRevoked())
                                .ifPresent(token -> {
                                        token.setRevokedAt(LocalDateTime.now());
                                        refreshTokenRepository.save(token);
                                });
        }

        @Transactional(readOnly = true)
        public CurrentUserResponseDTO getCurrentUser(Long userId) {
                UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                return CurrentUserResponseDTO.fromEntity(user);
        }

        private AuthResponseDTO createTokenResponse(
                        UserEntity user,
                        String clientIp) {
                UserDetailsImpl userDetails = UserDetailsImpl.build(user);

                String accessToken = jwtUtil.generateAccessToken(userDetails);

                UUID refreshTokenId = UUID.randomUUID();

                String refreshToken = jwtUtil.generateRefreshToken(
                                userDetails,
                                refreshTokenId);

                RefreshTokenEntity refreshTokenEntity = buildRefreshTokenEntity(
                                user,
                                refreshTokenId,
                                refreshToken,
                                clientIp);

                refreshTokenRepository.save(refreshTokenEntity);

                return new AuthResponseDTO(
                                accessToken,
                                refreshToken,
                                "Bearer",
                                jwtProperties.getAccessExpiration() / 1000,
                                CurrentUserResponseDTO.fromEntity(user));
        }

        private RefreshTokenEntity buildRefreshTokenEntity(
                        UserEntity user,
                        UUID tokenId,
                        String rawRefreshToken,
                        String clientIp) {
                RefreshTokenEntity entity = new RefreshTokenEntity();

                entity.setTokenId(tokenId);
                entity.setUser(user);
                entity.setTokenHash(jwtUtil.hashToken(rawRefreshToken));
                entity.setExpiresAt(
                                LocalDateTime.now().plus(
                                                Duration.ofMillis(
                                                                jwtProperties.getRefreshExpiration())));
                entity.setCreatedByIp(clientIp);

                return entity;
        }

        private void revokeActiveTokens(Long userId) {
                List<RefreshTokenEntity> activeTokens = refreshTokenRepository
                                .findByUserIdAndRevokedAtIsNull(userId);

                LocalDateTime now = LocalDateTime.now();

                activeTokens.stream()
                                .filter(token -> !token.isExpired())
                                .forEach(token -> token.setRevokedAt(now));

                refreshTokenRepository.saveAll(activeTokens);
        }

        private String normalizeEmail(String email) {
                return email.trim().toLowerCase(Locale.ROOT);
        }

        private ResponseStatusException invalidRefreshToken() {
                return new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Refresh token inválido o expirado");
        }
}