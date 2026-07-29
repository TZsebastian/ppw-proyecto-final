package ec.edu.ups.icc.academic_events.sessions.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SessionCreateRequestDTO(

        @NotBlank(message = "El título es obligatorio")
        @Size(
                max = 160,
                message = "El título no puede superar 160 caracteres"
        )
        String title,

        String description,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime startAt,

        @NotNull(message = "La fecha de finalización es obligatoria")
        LocalDateTime endAt,

        @Size(
                max = 200,
                message = "La ubicación no puede superar 200 caracteres"
        )
        String location,

        @Size(
                max = 500,
                message = "La URL virtual no puede superar 500 caracteres"
        )
        String virtualUrl
) {
}