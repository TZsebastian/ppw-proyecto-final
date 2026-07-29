package ec.edu.ups.icc.academic_events.events.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventUpdateRequestDTO(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar 160 caracteres")
        String title,

        @NotBlank(message = "La descripción es obligatoria")
        String description,

        @NotBlank(message = "La modalidad es obligatoria")
        @Pattern(
                regexp = "PRESENTIAL|VIRTUAL|HYBRID",
                message = "La modalidad debe ser PRESENTIAL, VIRTUAL o HYBRID"
        )
        String modality,

        @Size(max = 200, message = "La ubicación no puede superar 200 caracteres")
        String location,

        @Size(max = 500, message = "La URL virtual no puede superar 500 caracteres")
        String virtualUrl,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser mayor que cero")
        Integer capacity,

        @NotNull(message = "El inicio de inscripciones es obligatorio")
        LocalDateTime registrationStartAt,

        @NotNull(message = "El fin de inscripciones es obligatorio")
        LocalDateTime registrationEndAt,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime startAt,

        @NotNull(message = "La fecha de finalización es obligatoria")
        LocalDateTime endAt,

        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        @NotBlank(message = "El estado es obligatorio")
        @Pattern(
                regexp = "DRAFT|PUBLISHED|FINISHED|CANCELLED",
                message = "Estado de evento no permitido"
        )
        String status,

        @NotNull(message = "La versión del evento es obligatoria")
        @Min(value = 0, message = "La versión no puede ser negativa")
        Long version
) {
}