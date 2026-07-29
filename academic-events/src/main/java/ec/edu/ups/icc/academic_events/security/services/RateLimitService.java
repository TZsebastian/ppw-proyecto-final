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
public class RateLimitService {

    private static final long LOGIN_LIMIT = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);

    private static final long REGISTER_LIMIT = 3;
    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public void checkLoginLimit(
            String clientIp,
            String email
    ) {
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        checkLimit(
                "rate-limit:login:ip:" + clientIp,
                LOGIN_LIMIT,
                LOGIN_WINDOW,
                "Demasiados intentos de inicio de sesión desde esta IP"
        );

        checkLimit(
                "rate-limit:login:email:" + normalizedEmail,
                LOGIN_LIMIT,
                LOGIN_WINDOW,
                "Demasiados intentos de inicio de sesión para este correo"
        );
    }

    public void checkRegisterLimit(String clientIp) {
        checkLimit(
                "rate-limit:register:ip:" + clientIp,
                REGISTER_LIMIT,
                REGISTER_WINDOW,
                "Demasiados intentos de registro desde esta IP"
        );
    }

    private void checkLimit(
            String key,
            long maximumRequests,
            Duration window,
            String message
    ) {
        Long attempts = redisTemplate.opsForValue()
                .increment(key);

        if (attempts == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo comprobar el límite de solicitudes"
            );
        }

        if (attempts == 1) {
            redisTemplate.expire(key, window);
        }

        if (attempts > maximumRequests) {
            Long retryAfter = redisTemplate.getExpire(
                    key,
                    TimeUnit.SECONDS
            );

            long seconds = retryAfter != null && retryAfter > 0
                    ? retryAfter
                    : window.toSeconds();

            throw new RateLimitExceededException(
                    message,
                    seconds
            );
        }
    }
}