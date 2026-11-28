package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.config.EsewaProperties;
import in.dipesh.spring_backend.dto.EsewaPaymentForm;
import in.dipesh.spring_backend.dto.EsewaPaymentResponse;
import in.dipesh.spring_backend.dto.EsewaStatusResponse;
import in.dipesh.spring_backend.dto.PaymentResult;
import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.EsewaPaymentService;
import in.dipesh.spring_backend.service.EsewaSignatureService;
import in.dipesh.spring_backend.service.EsewaStatusClient;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EsewaPaymentServiceImpl implements EsewaPaymentService {
    private final OrderRepository orderRepository;
    private final EsewaProperties esewaProperties;
    private final EsewaSignatureService esewaSignatureService;
    private final EsewaStatusClient esewaStatusClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EsewaPaymentResponse initiatePayment(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid");
        }
        if (order.getPaymentStatus() == PaymentStatus.PENDING
                && order.getPaymentMethod() != PaymentMethod.ESEWA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another payment for this order is already pending");
        }

        order.setPaymentMethod(PaymentMethod.ESEWA);
        order.setPaymentStatus(PaymentStatus.PENDING);
        OrderEntity savedOrder = orderRepository.save(order);

        String signature = esewaSignatureService.createSignature(
                savedOrder.getGrandTotal(),
                savedOrder.getOrderId());

        EsewaPaymentForm form = EsewaPaymentForm.builder()
                .amount(savedOrder.getSubTotal().setScale(2, java.math.RoundingMode.HALF_UP))
                .taxAmount(savedOrder.getTax().setScale(2, java.math.RoundingMode.HALF_UP))
                .totalAmount(savedOrder.getGrandTotal().setScale(2, java.math.RoundingMode.HALF_UP))
                .productServiceCharge(java.math.BigDecimal.ZERO.setScale(2))
                .productDeliveryCharge(java.math.BigDecimal.ZERO.setScale(2))
                .transactionUuid(savedOrder.getOrderId())
                .productCode(esewaProperties.productCode())
                .signedFieldNames(EsewaSignatureService.SIGNED_FIELD_NAMES)
                .signature(signature)
                .successUrl(esewaProperties.successUrl())
                .failureUrl(esewaProperties.failureUrl()
                        + "?transaction_uuid=" + savedOrder.getOrderId())
                .build();

        return EsewaPaymentResponse.builder()
                .paymentUrl(esewaProperties.baseUrl() + "/api/epay/main/v2/form")
                .form(form)
                .build();
    }

    @Override
    @Transactional
    public PaymentResult verifyPayment(String encodedData) {
        JsonNode data;
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedData);
            data = objectMapper.readTree(
                    new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid eSewa success response",
                    exception);
        }

        if (!esewaSignatureService.isValidResponseSignature(data)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "eSewa response signature is invalid");
        }

        String orderId = data.path("transaction_uuid").asText();
        String productCode = data.path("product_code").asText();
        if (orderId.isBlank() || productCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "eSewa response is missing transaction details");
        }

        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        if (!esewaProperties.productCode().equals(productCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "eSewa product code does not match");
        }

        return refreshOrderStatus(order);
    }

    @Override
    @Transactional
    public PaymentResult refreshPaymentStatus(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        if (order.getPaymentMethod() != PaymentMethod.ESEWA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order does not have an eSewa payment");
        }
        return refreshOrderStatus(order);
    }

    private PaymentResult refreshOrderStatus(OrderEntity order) {
        if (order.getPaymentMethod() != PaymentMethod.ESEWA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order does not have an eSewa payment");
        }
        EsewaStatusResponse verification;
        try {
            verification = esewaStatusClient.verify(
                    order.getOrderId(),
                    esewaProperties.productCode(),
                    order.getGrandTotal());
        } catch (ResponseStatusException exception) {
            return new PaymentResult(
                    order.getOrderId(),
                    order.getPaymentStatus(),
                    "Payment status could not be verified yet. Please check again.");
        }

        if (verification == null
                || !order.getOrderId().equals(verification.transactionUuid())
                || !esewaProperties.productCode().equals(verification.productCode())
                || verification.totalAmount() == null
                || verification.totalAmount().compareTo(order.getGrandTotal()) != 0) {
            return new PaymentResult(
                    order.getOrderId(),
                    order.getPaymentStatus(),
                    "Payment status could not be verified yet. Please check again.");
        }

        String providerStatus = verification.status();
        PaymentStatus nextStatus = order.getPaymentStatus();
        if ("COMPLETE".equalsIgnoreCase(providerStatus)) {
            nextStatus = PaymentStatus.PAID;
        } else if ("NOT_FOUND".equalsIgnoreCase(providerStatus)
                || "CANCELED".equalsIgnoreCase(providerStatus)) {
            if (nextStatus != PaymentStatus.PAID) {
                nextStatus = PaymentStatus.FAILED;
            }
        }

        if (nextStatus != order.getPaymentStatus()) {
            order.setPaymentStatus(nextStatus);
            orderRepository.save(order);
        }

        String message = switch (nextStatus) {
            case PAID -> "Payment confirmed.";
            case FAILED -> "Payment was not completed.";
            case PENDING -> "Payment is still pending.";
            case UNPAID -> "Payment has not been initiated.";
        };
        return new PaymentResult(order.getOrderId(), nextStatus, message);
    }
}
