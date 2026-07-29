package ec.edu.ups.icc.academic_events.security.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(

        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {
}