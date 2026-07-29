package ec.edu.ups.icc.academic_events.users.services;

import ec.edu.ups.icc.academic_events.security.entities.RoleEntity;
import ec.edu.ups.icc.academic_events.security.repositories.RoleRepository;
import ec.edu.ups.icc.academic_events.users.dtos.UserCreateRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserResponseDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserRolesRequestDTO;
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

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final Set<String> VALID_ROLES =
            Set.of("ADMIN", "ORGANIZER", "PARTICIPANT");

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
        user.setFirstName(normalizeRequiredText(
                request.firstName(),
                "El nombre es obligatorio"
        ));
        user.setLastName(normalizeRequiredText(
                request.lastName(),
                "El apellido es obligatorio"
        ));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setStatus(STATUS_ACTIVE);
        user.setRoles(resolveRoles(request.roles()));

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponseDTO update(
            Long id,
            UserUpdateRequestDTO request
    ) {
        UserEntity user = findEntityById(id);
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(
                normalizedEmail,
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otro usuario con ese correo"
            );
        }

        user.setFirstName(normalizeRequiredText(
                request.firstName(),
                "El nombre es obligatorio"
        ));
        user.setLastName(normalizeRequiredText(
                request.lastName(),
                "El apellido es obligatorio"
        ));
        user.setEmail(normalizedEmail);
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponseDTO updateRoles(
            Long id,
            UserRolesRequestDTO request
    ) {
        UserEntity user = findEntityById(id);

        Set<RoleEntity> newRoles = resolveRoles(request.roles());

        validateRemovingLastAdministrator(user, newRoles);

        user.setRoles(newRoles);
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponseDTO block(
            Long id,
            String authenticatedEmail
    ) {
        UserEntity user = findEntityById(id);

        validateCannotBlockSelf(user, authenticatedEmail);

        if (STATUS_BLOCKED.equals(user.getStatus())) {
            return userMapper.toResponse(user);
        }

        validateNotLastActiveAdministrator(user);

        user.setStatus(STATUS_BLOCKED);
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponseDTO unblock(Long id) {
        UserEntity user = findEntityById(id);

        if (STATUS_ACTIVE.equals(user.getStatus())) {
            return userMapper.toResponse(user);
        }

        user.setStatus(STATUS_ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    private UserEntity findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    private Set<RoleEntity> resolveRoles(
            Set<String> requestedRoles
    ) {
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
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "El rol " + roleName
                                                + " no existe"
                                )
                        ))
                .collect(Collectors.toSet());
    }

    private void validateCannotBlockSelf(
            UserEntity targetUser,
            String authenticatedEmail
    ) {
        if (authenticatedEmail == null
                || authenticatedEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado"
            );
        }

        if (targetUser.getEmail().equalsIgnoreCase(
                authenticatedEmail.trim()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un administrador no puede bloquearse a sí mismo"
            );
        }
    }

    private void validateNotLastActiveAdministrator(
            UserEntity user
    ) {
        boolean isAdministrator = hasRole(user, ROLE_ADMIN);
        boolean isActive = STATUS_ACTIVE.equals(user.getStatus());

        if (!isAdministrator || !isActive) {
            return;
        }

        long activeAdministrators =
                userRepository
                        .countDistinctByStatusAndRoles_Name(
                                STATUS_ACTIVE,
                                ROLE_ADMIN
                        );

        if (activeAdministrators <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede bloquear al último administrador activo"
            );
        }
    }

    private void validateRemovingLastAdministrator(
            UserEntity user,
            Set<RoleEntity> newRoles
    ) {
        boolean currentlyAdministrator =
                hasRole(user, ROLE_ADMIN);

        boolean remainsAdministrator = newRoles.stream()
                .anyMatch(role ->
                        ROLE_ADMIN.equals(role.getName())
                );

        boolean isActive =
                STATUS_ACTIVE.equals(user.getStatus());

        if (!currentlyAdministrator
                || remainsAdministrator
                || !isActive) {
            return;
        }

        long activeAdministrators =
                userRepository
                        .countDistinctByStatusAndRoles_Name(
                                STATUS_ACTIVE,
                                ROLE_ADMIN
                        );

        if (activeAdministrators <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede retirar el rol ADMIN "
                            + "al último administrador activo"
            );
        }
    }

    private boolean hasRole(
            UserEntity user,
            String roleName
    ) {
        return user.getRoles()
                .stream()
                .anyMatch(role ->
                        roleName.equals(role.getName())
                );
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

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo es obligatorio"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredText(
            String value,
            String errorMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }

        return value.trim().replaceAll("\\s+", " ");
    }
}