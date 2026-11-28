package in.dipesh.spring_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "khalti")
public record KhaltiProperties(
        String baseUrl,
        String secretKey,
        String returnUrl,
        String websiteUrl,
        String frontendUrl
) {
}
