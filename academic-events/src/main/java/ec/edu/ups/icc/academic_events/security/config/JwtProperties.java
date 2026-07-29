package ec.edu.ups.icc.academic_events.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private Long accessExpiration;
    private Long refreshExpiration;
    private String issuer;
    private String header;
    private String prefix;
}