package ec.edu.ups.icc.academic_events.categories.dtos;

import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequestDTO(

        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
}