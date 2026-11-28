package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.OrderItemRequest;
import in.dipesh.spring_backend.dto.OrderRequest;
import in.dipesh.spring_backend.dto.OrderResponse;
import in.dipesh.spring_backend.dto.OrderReceipt;
import in.dipesh.spring_backend.dto.OrderHistoryEntry;
import in.dipesh.spring_backend.entity.ItemEntity;
import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.OrderItemEntity;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.ItemRepository;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.02");

    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryEntry> getPaidOrders() {
        return orderRepository.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID)
                .stream().map(order -> new OrderHistoryEntry(order.getOrderId(), order.getCreatedAt(),
                        order.getCustomerName(), order.getPhoneNumber(), order.getPaymentMethod(),
                        order.getPaymentStatus(), "NPR", order.getGrandTotal())).toList();
    }

    @Override
    @Transactional
    public OrderReceipt getReceipt(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A receipt is available only after the stored order is confirmed paid");
        }
        String reference = order.getPaymentMethod() == PaymentMethod.STRIPE
                ? order.getStripeSessionId() : order.getPaymentMethod() == PaymentMethod.KHALTI
                ? order.getKhaltiPidx() : order.getPaymentMethod() == PaymentMethod.ESEWA
                ? order.getOrderId() : null;
        return new OrderReceipt(order.getOrderId(), order.getCreatedAt(), order.getCustomerName(),
                order.getPhoneNumber(), "NPR", order.getPaymentMethod(), order.getPaymentStatus(),
                reference, order.getSubTotal(), order.getTax(), order.getGrandTotal(),
                order.getItems().stream().map(item -> new OrderReceipt.Item(item.getItemId(),
                        item.getName(), item.getPrice(), item.getQuantity(),
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).toList());
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        return createOrder(request, null, PaymentStatus.UNPAID);
    }

    @Override
    @Transactional
    public OrderResponse createCashOrder(OrderRequest request) {
        return createOrder(request, PaymentMethod.CASH, PaymentStatus.PAID);
    }

    private OrderResponse createOrder(
            OrderRequest request,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus
    ) {
        List<OrderItemEntity> orderItems = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;

        for (OrderItemRequest requestedItem : request.getItems()) {
            ItemEntity item = itemRepository.findByItemId(requestedItem.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Item not found: " + requestedItem.getItemId()));

            BigDecimal itemPrice = item.getPrice();
            subTotal = subTotal.add(itemPrice.multiply(BigDecimal.valueOf(requestedItem.getQuantity())));

            orderItems.add(OrderItemEntity.builder()
                    .itemId(item.getItemId())
                    .name(item.getName())
                    .price(itemPrice)
                    .quantity(requestedItem.getQuantity())
                    .build());
        }

        BigDecimal tax = subTotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subTotal.add(tax);

        OrderEntity order = OrderEntity.builder()
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .items(orderItems)
                .subTotal(subTotal)
                .tax(tax)
                .grandTotal(grandTotal)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .build();

        OrderEntity savedOrder = orderRepository.save(order);

        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .subTotal(savedOrder.getSubTotal())
                .tax(savedOrder.getTax())
                .grandTotal(savedOrder.getGrandTotal())
                .paymentStatus(savedOrder.getPaymentStatus())
                .paymentMethod(savedOrder.getPaymentMethod())
                .build();
    }
}
