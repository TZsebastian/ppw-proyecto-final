package ec.edu.ups.icc.academic_events.users.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record UserRolesRequestDTO(

        @NotEmpty(message = "Debe asignarse al menos un rol")
        Set<
                @Pattern(
                        regexp = "ADMIN|ORGANIZER|PARTICIPANT",
                        message = "Rol no permitido"
                )
                String
        > roles
) {
}