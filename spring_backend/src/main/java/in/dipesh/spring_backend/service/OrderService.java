package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.OrderRequest;
import in.dipesh.spring_backend.dto.OrderResponse;
import in.dipesh.spring_backend.dto.OrderReceipt;
import in.dipesh.spring_backend.dto.OrderHistoryEntry;

public interface OrderService {
    java.util.List<OrderHistoryEntry> getPaidOrders();
    OrderReceipt getReceipt(String orderId);
    OrderResponse createOrder(OrderRequest request);
    OrderResponse createCashOrder(OrderRequest request);
}
