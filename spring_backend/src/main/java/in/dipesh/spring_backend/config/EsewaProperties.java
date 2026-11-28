package in.dipesh.spring_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "esewa")
public record EsewaProperties(
        String baseUrl,
        String statusUrl,
        String productCode,
        String secretKey,
        String successUrl,
        String failureUrl,
        String frontendUrl
) {
}
