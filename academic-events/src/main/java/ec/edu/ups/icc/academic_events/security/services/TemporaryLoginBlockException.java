package ec.edu.ups.icc.academic_events.security.services;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class TemporaryLoginBlockException
        extends ResponseStatusException {

    private final long retryAfterSeconds;

    public TemporaryLoginBlockException(
            String reason,
            long retryAfterSeconds
    ) {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                reason
        );

        this.retryAfterSeconds = retryAfterSeconds;
    }
}