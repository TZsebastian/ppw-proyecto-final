package ec.edu.ups.icc.academic_events.users.services;

import ec.edu.ups.icc.academic_events.security.entities.RoleEntity;
import ec.edu.ups.icc.academic_events.security.repositories.RoleRepository;
import ec.edu.ups.icc.academic_events.users.dtos.UserCreateRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserResponseDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserRolesRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserStatusRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.users.entities.UserEntity;
import ec.edu.ups.icc.academic_events.users.mappers.UserMapper;
import ec.edu.ups.icc.academic_events.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> VALID_ROLES =
            Set.of("ADMIN", "ORGANIZER", "PARTICIPANT");

    private static final Set<String> VALID_STATUSES =
            Set.of("ACTIVE", "BLOCKED");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese correo"
            );
        }

        UserEntity user = new UserEntity();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setRoles(resolveRoles(request.roles()));

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO update(Long id, UserUpdateRequestDTO request) {
        UserEntity user = findEntityById(id);
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otro usuario con ese correo"
            );
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO updateStatus(
            Long id,
            UserStatusRequestDTO request
    ) {
        UserEntity user = findEntityById(id);
        String normalizedStatus = normalizeStatus(request.status());

        user.setStatus(normalizedStatus);
        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO updateRoles(
            Long id,
            UserRolesRequestDTO request
    ) {
        UserEntity user = findEntityById(id);

        user.setRoles(resolveRoles(request.roles()));
        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toResponse(userRepository.save(user));
    }

    private UserEntity findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    private Set<RoleEntity> resolveRoles(Set<String> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe asignarse al menos un rol"
            );
        }

        Set<String> normalizedRoles = requestedRoles.stream()
                .map(this::normalizeRole)
                .collect(Collectors.toSet());

        return normalizedRoles.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El rol " + roleName + " no existe"
                        )))
                .collect(Collectors.toSet());
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del rol es obligatorio"
            );
        }

        String normalizedRole = role.trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_ROLES.contains(normalizedRole)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rol no permitido: " + role
            );
        }

        return normalizedRole;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        String normalizedStatus = status.trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado debe ser ACTIVE o BLOCKED"
            );
        }

        return normalizedStatus;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}