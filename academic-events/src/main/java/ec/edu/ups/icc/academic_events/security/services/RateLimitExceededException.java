package ec.edu.ups.icc.academic_events.security.services;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class RateLimitExceededException
        extends ResponseStatusException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(
            String reason,
            long retryAfterSeconds
    ) {
        super(HttpStatus.TOO_MANY_REQUESTS, reason);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}