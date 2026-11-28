package in.dipesh.spring_backend.dto;

public record StripePaymentResponse(String sessionId, String paymentUrl) { }
