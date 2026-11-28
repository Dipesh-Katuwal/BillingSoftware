package in.dipesh.spring_backend.dto;

import java.math.BigDecimal;

public record StripeCheckoutOptions(String currency, BigDecimal minimumTotal) { }
