package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.OrderItemRequest;
import in.dipesh.spring_backend.dto.OrderRequest;
import in.dipesh.spring_backend.dto.OrderResponse;
import in.dipesh.spring_backend.entity.ItemEntity;
import in.dipesh.spring_backend.entity.OrderEntity;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderServiceImpl orderService = new OrderServiceImpl(itemRepository, orderRepository);

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
}
