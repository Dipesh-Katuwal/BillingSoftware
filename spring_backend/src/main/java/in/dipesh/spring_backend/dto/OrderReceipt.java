package in.dipesh.spring_backend.dto;

import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderReceipt(String orderId, LocalDateTime createdAt, String customerName,
        String phoneNumber, String currency, PaymentMethod paymentMethod,
        PaymentStatus paymentStatus, String paymentReference, BigDecimal subTotal,
        BigDecimal tax, BigDecimal grandTotal, List<Item> items) {
    public record Item(String itemId, String name, BigDecimal unitPrice,
                       Integer quantity, BigDecimal lineTotal) { }
}
