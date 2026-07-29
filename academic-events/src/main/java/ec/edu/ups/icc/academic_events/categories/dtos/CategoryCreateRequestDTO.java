package ec.edu.ups.icc.academic_events.categories.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequestDTO(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(
                max = 80,
                message = "El nombre no puede superar 80 caracteres"
        )
        String name,

        @Size(
                max = 255,
                message = "La descripción no puede superar 255 caracteres"
        )
        String description
) {
}