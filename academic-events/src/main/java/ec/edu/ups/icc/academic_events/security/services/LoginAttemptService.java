package ec.edu.ups.icc.academic_events.security.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final long MAX_FAILED_ATTEMPTS = 5;

    private static final Duration ATTEMPT_WINDOW =
            Duration.ofMinutes(15);

    private static final Duration BLOCK_DURATION =
            Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public void checkBlocked(
            String clientIp,
            String email
    ) {
        String normalizedEmail = normalizeEmail(email);

        String emailBlockKey =
                "security:login:block:email:" + normalizedEmail;

        String ipBlockKey =
                "security:login:block:ip:" + clientIp;

        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(emailBlockKey)
        )) {
            throwBlockedException(
                    emailBlockKey,
                    "El usuario está bloqueado temporalmente"
            );
        }

        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(ipBlockKey)
        )) {
            throwBlockedException(
                    ipBlockKey,
                    "La dirección IP está bloqueada temporalmente"
            );
        }
    }

    public void registerFailure(
            String clientIp,
            String email
    ) {
        String normalizedEmail = normalizeEmail(email);

        String emailAttemptsKey =
                "security:login:attempts:email:"
                        + normalizedEmail;

        String ipAttemptsKey =
                "security:login:attempts:ip:"
                        + clientIp;

        long emailAttempts =
                incrementAttempts(emailAttemptsKey);

        long ipAttempts =
                incrementAttempts(ipAttemptsKey);

        if (emailAttempts >= MAX_FAILED_ATTEMPTS) {
            block(
                    "security:login:block:email:"
                            + normalizedEmail
            );

            redisTemplate.delete(emailAttemptsKey);
        }

        if (ipAttempts >= MAX_FAILED_ATTEMPTS) {
            block(
                    "security:login:block:ip:"
                            + clientIp
            );

            redisTemplate.delete(ipAttemptsKey);
        }
    }

    public void registerSuccess(
            String clientIp,
            String email
    ) {
        String normalizedEmail = normalizeEmail(email);

        redisTemplate.delete(
                "security:login:attempts:email:"
                        + normalizedEmail
        );

        redisTemplate.delete(
                "security:login:attempts:ip:"
                        + clientIp
        );
    }

    private long incrementAttempts(String key) {
        Long attempts = redisTemplate
                .opsForValue()
                .increment(key);

        if (attempts == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo registrar el intento de inicio de sesión"
            );
        }

        if (attempts == 1) {
            redisTemplate.expire(
                    key,
                    ATTEMPT_WINDOW
            );
        }

        return attempts;
    }

    private void block(String key) {
        redisTemplate.opsForValue().set(
                key,
                "BLOCKED",
                BLOCK_DURATION
        );
    }

    private void throwBlockedException(
            String blockKey,
            String message
    ) {
        Long retryAfter = redisTemplate.getExpire(
                blockKey,
                TimeUnit.SECONDS
        );

        long seconds =
                retryAfter != null && retryAfter > 0
                        ? retryAfter
                        : BLOCK_DURATION.toSeconds();

        throw new TemporaryLoginBlockException(
                message,
                seconds
        );
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}