package ec.edu.ups.icc.academic_events.security.dtos;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        CurrentUserResponseDTO user
) {
}