package ec.edu.ups.icc.academic_events.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequestDTO(

        @NotBlank(message = "El estado es obligatorio")
        @Pattern(
                regexp = "ACTIVE|BLOCKED",
                message = "El estado debe ser ACTIVE o BLOCKED"
        )
        String status
) {
}