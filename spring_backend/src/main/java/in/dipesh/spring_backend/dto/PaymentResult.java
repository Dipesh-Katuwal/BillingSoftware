package in.dipesh.spring_backend.dto;

import in.dipesh.spring_backend.entity.PaymentStatus;

public record PaymentResult(
        String orderId,
        PaymentStatus paymentStatus,
        String message
) {
}
