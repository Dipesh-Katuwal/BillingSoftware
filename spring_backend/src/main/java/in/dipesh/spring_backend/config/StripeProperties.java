package in.dipesh.spring_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(String testSecretKey, String webhookSecret, String frontendUrl,
                               BigDecimal minimumTotalNpr) {
    @ConstructorBinding
    public StripeProperties {
        if (minimumTotalNpr == null) minimumTotalNpr = new BigDecimal("100.00");
        if (minimumTotalNpr.signum() <= 0 || minimumTotalNpr.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Stripe minimum total must be a positive exact NPR paisa amount");
        }
    }

    public StripeProperties(String testSecretKey, String webhookSecret, String frontendUrl) {
        this(testSecretKey, webhookSecret, frontendUrl, new BigDecimal("100.00"));
    }
}
