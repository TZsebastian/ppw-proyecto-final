package ec.edu.ups.icc.academic_events.users.mappers;

import ec.edu.ups.icc.academic_events.users.dtos.UserResponseDTO;
import ec.edu.ups.icc.academic_events.users.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(UserEntity user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toUnmodifiableSet());

        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}