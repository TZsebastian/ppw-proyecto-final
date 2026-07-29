package ec.edu.ups.icc.academic_events.users.dtos;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status,
        Set<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}