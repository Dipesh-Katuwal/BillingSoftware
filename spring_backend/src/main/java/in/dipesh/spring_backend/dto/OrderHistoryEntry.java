package in.dipesh.spring_backend.dto;

import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderHistoryEntry(String orderId, LocalDateTime createdAt, String customerName,
        String phoneNumber, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
        String currency, BigDecimal grandTotal) { }
