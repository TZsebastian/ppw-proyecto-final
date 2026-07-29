package ec.edu.ups.icc.academic_events.security.dtos;

import ec.edu.ups.icc.academic_events.users.entities.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

public record CurrentUserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status,
        Set<String> roles
) {

    public static CurrentUserResponseDTO fromEntity(UserEntity user) {
        Set<String> roleNames = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        return new CurrentUserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus(),
                roleNames
        );
    }
}