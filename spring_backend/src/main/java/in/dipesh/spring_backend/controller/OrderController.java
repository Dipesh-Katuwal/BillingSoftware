package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.dto.OrderRequest;
import in.dipesh.spring_backend.dto.OrderResponse;
import in.dipesh.spring_backend.dto.OrderReceipt;
import in.dipesh.spring_backend.dto.OrderHistoryEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import in.dipesh.spring_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/orders/paid")
    public java.util.List<OrderHistoryEntry> paidOrders() {
        return orderService.getPaidOrders();
    }

    @GetMapping("/orders/{orderId}/receipt")
    public OrderReceipt receipt(@PathVariable String orderId) {
        return orderService.getReceipt(orderId);
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @PostMapping("/orders/cash")
    public ResponseEntity<OrderResponse> createCashOrder(
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createCashOrder(request));
    }
}
