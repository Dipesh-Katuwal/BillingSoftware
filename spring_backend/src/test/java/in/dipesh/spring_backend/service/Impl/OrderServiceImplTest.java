package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.OrderItemRequest;
import in.dipesh.spring_backend.dto.OrderRequest;
import in.dipesh.spring_backend.dto.OrderResponse;
import in.dipesh.spring_backend.entity.ItemEntity;
import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.OrderItemEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.ItemRepository;
import in.dipesh.spring_backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderServiceImpl orderService = new OrderServiceImpl(itemRepository, orderRepository);

    @Test
    void historyRequestsPaidOrdersAndMapsSavedTotalsWithoutLoadingCatalog() {
        var paid = OrderEntity.builder().orderId("ORD-history").customerName("Alex")
                .phoneNumber("9800000000").paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PAID).grandTotal(new BigDecimal("117.30")).build();
        when(orderRepository.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID))
                .thenReturn(List.of(paid));
        var history = orderService.getPaidOrders();
        assertEquals(1, history.size());
        assertEquals("ORD-history", history.get(0).orderId());
        assertEquals("Alex", history.get(0).customerName());
        assertEquals(PaymentMethod.CASH, history.get(0).paymentMethod());
        assertEquals(PaymentStatus.PAID, history.get(0).paymentStatus());
        assertEquals("NPR", history.get(0).currency());
        assertEquals(new BigDecimal("117.30"), history.get(0).grandTotal());
        verifyNoInteractions(itemRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void historySupportsAnEmptyPaidOrderList() {
        when(orderRepository.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID))
                .thenReturn(List.of());
        assertEquals(List.of(), orderService.getPaidOrders());
    }

    @Test
    void receiptUsesSavedPaidOrderAndItemSnapshotsWithoutCreatingAnotherOrder() {
        var date = java.time.LocalDateTime.of(2026, 11, 28, 22, 54, 42);
        var order = OrderEntity.builder().orderId("ORD-receipt").createdAt(date)
                .customerName("Alex").phoneNumber("9800000000")
                .paymentMethod(PaymentMethod.STRIPE).paymentStatus(PaymentStatus.PAID)
                .stripeSessionId("cs_test_receipt").subTotal(new BigDecimal("115.00"))
                .tax(new BigDecimal("2.30")).grandTotal(new BigDecimal("117.30"))
                .items(List.of(OrderItemEntity.builder().itemId("tea").name("Saved tea name")
                        .price(new BigDecimal("23.00")).quantity(5).build())).build();
        when(orderRepository.findByOrderId("ORD-receipt")).thenReturn(Optional.of(order));
        var receipt = orderService.getReceipt("ORD-receipt");
        assertEquals("Alex", receipt.customerName());
        assertEquals("9800000000", receipt.phoneNumber());
        assertEquals(date, receipt.createdAt());
        assertEquals("NPR", receipt.currency());
        assertEquals(PaymentStatus.PAID, receipt.paymentStatus());
        assertEquals("cs_test_receipt", receipt.paymentReference());
        assertEquals(new BigDecimal("117.30"), receipt.grandTotal());
        assertEquals("Saved tea name", receipt.items().get(0).name());
        assertEquals(5, receipt.items().get(0).quantity());
        assertEquals(new BigDecimal("115.00"), receipt.items().get(0).lineTotal());
        verifyNoInteractions(itemRepository);
        verify(orderRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"UNPAID", "PENDING", "FAILED"})
    void receiptRequiresPersistedPaidStatus(PaymentStatus status) {
        when(orderRepository.findByOrderId("ORD-unpaid"))
                .thenReturn(Optional.of(OrderEntity.builder().paymentStatus(status).build()));
        var exception = assertThrows(ResponseStatusException.class, () -> orderService.getReceipt("ORD-unpaid"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void receiptRejectsMissingOrdersAndSupportsCashWithoutProviderReference() {
        when(orderRepository.findByOrderId("missing")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> orderService.getReceipt("missing")).getStatusCode());
        when(orderRepository.findByOrderId("ORD-cash")).thenReturn(Optional.of(OrderEntity.builder()
                .orderId("ORD-cash").paymentMethod(PaymentMethod.CASH).paymentStatus(PaymentStatus.PAID).build()));
        assertNull(orderService.getReceipt("ORD-cash").paymentReference());
    }

    @Test
    void createOrderUsesCatalogPriceAndCalculatesTax() {
        ItemEntity catalogItem = ItemEntity.builder()
                .itemId("item-123")
                .name("Tea")
                .price(new BigDecimal("100.00"))
                .build();
        when(itemRepository.findByItemId("item-123")).thenReturn(Optional.of(catalogItem));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setOrderId("ORD-1");
            return order;
        });

        OrderResponse response = orderService.createOrder(new OrderRequest(
                "Alex",
                "9800000000",
                List.of(new OrderItemRequest("item-123", 2))));

        assertEquals("ORD-1", response.getOrderId());
        assertEquals(new BigDecimal("200.00"), response.getSubTotal());
        assertEquals(new BigDecimal("4.00"), response.getTax());
        assertEquals(new BigDecimal("204.00"), response.getGrandTotal());
        assertEquals(PaymentStatus.UNPAID, response.getPaymentStatus());
        verify(itemRepository).findByItemId("item-123");
    }

    @Test
    void createOrderRejectsUnknownCatalogItems() {
        when(itemRepository.findByItemId("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(new OrderRequest(
                        "Alex",
                        "9800000000",
                        List.of(new OrderItemRequest("missing", 1)))));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void cashOrderIsRecordedAsPaidWithCashPaymentMethod() {
        ItemEntity catalogItem = ItemEntity.builder()
                .itemId("item-123")
                .name("Tea")
                .price(new BigDecimal("100.00"))
                .build();
        when(itemRepository.findByItemId("item-123")).thenReturn(Optional.of(catalogItem));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setOrderId("ORD-CASH");
            return order;
        });

        OrderResponse response = orderService.createCashOrder(new OrderRequest(
                "Alex",
                "9800000000",
                List.of(new OrderItemRequest("item-123", 2))));

        assertEquals("ORD-CASH", response.getOrderId());
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertEquals(PaymentMethod.CASH, response.getPaymentMethod());
        assertEquals(new BigDecimal("204.00"), response.getGrandTotal());
    }
}
