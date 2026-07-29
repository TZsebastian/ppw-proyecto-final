package ec.edu.ups.icc.academic_events.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "swagger.security")
@Getter
@Setter
public class SwaggerSecurityProperties {

    private boolean enabled = false;

    private String username;

    private String password;
}