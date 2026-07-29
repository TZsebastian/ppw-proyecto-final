package ec.edu.ups.icc.academic_events.shared.exceptions;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {

    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                null
        );
    }

    public static ApiErrorResponse validation(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                validationErrors
        );
    }
}