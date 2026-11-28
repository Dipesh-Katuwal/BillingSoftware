package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.config.KhaltiProperties;
import in.dipesh.spring_backend.dto.KhaltiInitiateApiResponse;
import in.dipesh.spring_backend.dto.KhaltiInitiateRequest;
import in.dipesh.spring_backend.dto.KhaltiLookupResponse;
import in.dipesh.spring_backend.dto.KhaltiPaymentResponse;
import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.KhaltiApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KhaltiPaymentServiceImplTest {
    private final KhaltiProperties properties = new KhaltiProperties(
            "https://dev.khalti.com/api/v2",
            "test-secret",
            "http://localhost:8080/payments/khalti/callback",
            "http://localhost:5173",
            "http://localhost:5173");
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final KhaltiApiClient khaltiApiClient = mock(KhaltiApiClient.class);
    private final KhaltiPaymentServiceImpl paymentService =
            new KhaltiPaymentServiceImpl(
                    orderRepository,
                    properties,
                    khaltiApiClient);

    @Test
    void initiatesKhaltiPaymentUsingOrderTotalInPaisa() {
        OrderEntity order = order("ORD-123", PaymentStatus.UNPAID);
        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));
        when(khaltiApiClient.initiate(any(KhaltiInitiateRequest.class)))
                .thenReturn(new KhaltiInitiateApiResponse(
                        "pidx-123",
                        "https://test-pay.khalti.com/?pidx=pidx-123"));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KhaltiPaymentResponse response =
                paymentService.initiatePayment(order.getOrderId());

        assertEquals("pidx-123", response.pidx());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(PaymentMethod.KHALTI, order.getPaymentMethod());
        assertEquals("pidx-123", order.getKhaltiPidx());
        var request = org.mockito.ArgumentCaptor.forClass(KhaltiInitiateRequest.class);
        verify(khaltiApiClient).initiate(request.capture());
        assertEquals(37230, request.getValue().amount());
        assertEquals("ORD-123", request.getValue().purchaseOrderId());
    }

    @Test
    void returnsExistingKhaltiSessionInsteadOfCreatingAnother() {
        OrderEntity order = order("ORD-EXISTING", PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.KHALTI);
        order.setKhaltiPidx("pidx-existing");
        order.setKhaltiPaymentUrl("https://test-pay.khalti.com/?pidx=pidx-existing");
        when(orderRepository.findByOrderId(order.getOrderId()))
                .thenReturn(Optional.of(order));

        KhaltiPaymentResponse response =
                paymentService.initiatePayment(order.getOrderId());

        assertEquals("pidx-existing", response.pidx());
        verifyNoInteractions(khaltiApiClient);
    }

    @Test
    void marksOrderPaidOnlyAfterKhaltiLookupConfirmsMatchingAmount() {
        OrderEntity order = order("ORD-PAID", PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.KHALTI);
        order.setKhaltiPidx("pidx-paid");
        when(orderRepository.findByKhaltiPidx("pidx-paid"))
                .thenReturn(Optional.of(order));
        when(khaltiApiClient.lookup("pidx-paid"))
                .thenReturn(new KhaltiLookupResponse(
                        "pidx-paid",
                        37230L,
                        "Completed",
                        "txn-123",
                        false));

        var result = paymentService.verifyCallback("pidx-paid", "ORD-PAID");

        assertEquals(PaymentStatus.PAID, result.paymentStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void doesNotMarkOrderPaidWhenKhaltiAmountDoesNotMatch() {
        OrderEntity order = order("ORD-WRONG-AMOUNT", PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.KHALTI);
        order.setKhaltiPidx("pidx-wrong");
        when(orderRepository.findByKhaltiPidx("pidx-wrong"))
                .thenReturn(Optional.of(order));
        when(khaltiApiClient.lookup("pidx-wrong"))
                .thenReturn(new KhaltiLookupResponse(
                        "pidx-wrong",
                        100L,
                        "Completed",
                        "txn-123",
                        false));

        var result = paymentService.verifyCallback("pidx-wrong", "ORD-WRONG-AMOUNT");

        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void marksCanceledKhaltiPaymentFailed() {
        OrderEntity order = order("ORD-CANCELED", PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.KHALTI);
        order.setKhaltiPidx("pidx-canceled");
        when(orderRepository.findByKhaltiPidx("pidx-canceled"))
                .thenReturn(Optional.of(order));
        when(khaltiApiClient.lookup("pidx-canceled"))
                .thenReturn(new KhaltiLookupResponse(
                        "pidx-canceled",
                        37230L,
                        "User canceled",
                        null,
                        false));

        var result = paymentService.verifyCallback("pidx-canceled", "ORD-CANCELED");

        assertEquals(PaymentStatus.FAILED, result.paymentStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void rejectsKhaltiCallbackWithMismatchedOrderId() {
        OrderEntity order = order("ORD-REAL", PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.KHALTI);
        order.setKhaltiPidx("pidx-real");
        when(orderRepository.findByKhaltiPidx("pidx-real"))
                .thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.verifyCallback("pidx-real", "ORD-OTHER"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(khaltiApiClient);
    }

    @Test
    void doesNotVerifyOldKhaltiPaymentAfterOrderUsesAnotherMethod() {
        OrderEntity order = order("ORD-CHANGED-METHOD", PaymentStatus.FAILED);
        order.setPaymentMethod(PaymentMethod.ESEWA);
        order.setKhaltiPidx("pidx-old");
        when(orderRepository.findByKhaltiPidx("pidx-old"))
                .thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.verifyCallback("pidx-old", "ORD-CHANGED-METHOD"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(khaltiApiClient);
    }

    private OrderEntity order(String orderId, PaymentStatus status) {
        return OrderEntity.builder()
                .orderId(orderId)
                .customerName("Alex")
                .phoneNumber("9800000000")
                .grandTotal(new BigDecimal("372.30"))
                .paymentStatus(status)
                .build();
    }
}
