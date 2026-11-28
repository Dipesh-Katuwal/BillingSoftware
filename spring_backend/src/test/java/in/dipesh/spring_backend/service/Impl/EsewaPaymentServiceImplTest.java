package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.config.EsewaProperties;
import in.dipesh.spring_backend.dto.EsewaPaymentResponse;
import in.dipesh.spring_backend.dto.EsewaStatusResponse;
import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.EsewaSignatureService;
import in.dipesh.spring_backend.service.EsewaStatusClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EsewaPaymentServiceImplTest {

    private final EsewaProperties properties = new EsewaProperties(
            "https://sandbox.example",
            "https://status.example",
            "EPAYTEST",
            "test-secret",
            "http://localhost:8080/payments/esewa/success",
            "http://localhost:8080/payments/esewa/failure",
            "http://localhost:5173");
    private final EsewaSignatureService signatureService =
            new EsewaSignatureService(properties);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final EsewaStatusClient esewaStatusClient = mock(EsewaStatusClient.class);
    private final EsewaPaymentServiceImpl paymentService =
            new EsewaPaymentServiceImpl(
                    orderRepository,
                    properties,
                    signatureService,
                    esewaStatusClient,
                    new ObjectMapper());

    @Test
    void initiatesPaymentForUnpaidOrder() {
        OrderEntity order = OrderEntity.builder()
                .orderId("ORD1795837039284")
                .subTotal(new BigDecimal("440.00"))
                .tax(new BigDecimal("8.80"))
                .grandTotal(new BigDecimal("448.80"))
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EsewaPaymentResponse response =
                paymentService.initiatePayment(order.getOrderId());

        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(PaymentMethod.ESEWA, order.getPaymentMethod());
        assertEquals("448.80", response.getForm().getTotalAmount().toPlainString());
        assertEquals("ORD1795837039284",
                response.getForm().getTransactionUuid());
        assertEquals("EPAYTEST", response.getForm().getProductCode());
        verify(orderRepository).save(order);
    }

    @Test
    void rejectsAnAlreadyPaidOrder() {
        OrderEntity order = OrderEntity.builder()
                .orderId("ORD-PAID")
                .paymentStatus(PaymentStatus.PAID)
                .build();
        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.initiatePayment(order.getOrderId()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void marksOrderPaidOnlyAfterMockedEsewaVerificationCompletes() {
        OrderEntity order = OrderEntity.builder()
                .orderId("ORD-VERIFIED")
                .grandTotal(new BigDecimal("372.30"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));
        when(esewaStatusClient.verify(
                order.getOrderId(),
                "EPAYTEST",
                order.getGrandTotal()))
                .thenReturn(new EsewaStatusResponse(
                        "EPAYTEST",
                        "ORD-VERIFIED",
                        new BigDecimal("372.30"),
                        "COMPLETE",
                        "test-reference"));

        var result = paymentService.verifyPayment(encodeCallback(
                "ORD-VERIFIED",
                "EPAYTEST"));

        assertEquals("Payment confirmed.", result.message());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void keepsOrderPendingWhileMockedEsewaStatusIsPending() {
        OrderEntity order = OrderEntity.builder()
                .orderId("ORD-INCOMPLETE")
                .grandTotal(new BigDecimal("372.30"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));
        when(esewaStatusClient.verify(
                order.getOrderId(),
                "EPAYTEST",
                order.getGrandTotal()))
                .thenReturn(new EsewaStatusResponse(
                        "EPAYTEST",
                        "ORD-INCOMPLETE",
                        new BigDecimal("372.30"),
                        "PENDING",
                        null));

        var result = paymentService.verifyPayment(encodeCallback(
                "ORD-INCOMPLETE",
                "EPAYTEST"));

        assertEquals("Payment is still pending.", result.message());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void doesNotMarkOrderPaidWhenMockedAmountDoesNotMatch() {
        OrderEntity order = OrderEntity.builder()
                .orderId("ORD-WRONG-AMOUNT")
                .grandTotal(new BigDecimal("372.30"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));
        when(esewaStatusClient.verify(
                order.getOrderId(),
                "EPAYTEST",
                order.getGrandTotal()))
                .thenReturn(new EsewaStatusResponse(
                        "EPAYTEST",
                        "ORD-WRONG-AMOUNT",
                        new BigDecimal("12.00"),
                        "COMPLETE",
                        "test-reference"));

        var result = paymentService.verifyPayment(encodeCallback(
                "ORD-WRONG-AMOUNT",
                "EPAYTEST"));

        assertEquals("Payment status could not be verified yet. Please check again.",
                result.message());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCallbackWithInvalidSignature() {
        String encodedData = encodeCallback(
                "ORD-BAD-SIGNATURE",
                "EPAYTEST",
                false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.verifyPayment(encodedData));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(esewaStatusClient);
    }

    private String encodeCallback(String orderId, String productCode) {
        return encodeCallback(orderId, productCode, true);
    }

    private String encodeCallback(
            String orderId,
            String productCode,
            boolean validSignature
    ) {
        String signedFieldNames =
                "transaction_code,status,total_amount,transaction_uuid,product_code,signed_field_names";
        String signedPayload = "transaction_code=TEST-REF"
                + ",status=COMPLETE"
                + ",total_amount=372.30"
                + ",transaction_uuid=" + orderId
                + ",product_code=" + productCode
                + ",signed_field_names=" + signedFieldNames;
        String signature = validSignature ? signature(signedPayload) : "invalid-signature";
        String callback = """
                {"transaction_code":"TEST-REF","status":"COMPLETE","total_amount":"372.30","transaction_uuid":"%s","product_code":"%s","signed_field_names":"%s","signature":"%s"}
                """.formatted(orderId, productCode, signedFieldNames, signature);
        return Base64.getEncoder().encodeToString(
                callback.getBytes(StandardCharsets.UTF_8));
    }

    private String signature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    "test-secret".getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
