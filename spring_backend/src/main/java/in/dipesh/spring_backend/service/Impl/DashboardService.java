package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.DashboardResponse;
import in.dipesh.spring_backend.dto.OrderHistoryEntry;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.CategoryRepository;
import in.dipesh.spring_backend.repository.ItemRepository;
import in.dipesh.spring_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kathmandu"));
        var paidOrders = orderRepository.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID);
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal todaySales = BigDecimal.ZERO;
        int todayPaidOrders = 0;
        var recentOrders = new ArrayList<OrderHistoryEntry>();

        for (var order : paidOrders) {
            BigDecimal amount = order.getGrandTotal() == null ? BigDecimal.ZERO : order.getGrandTotal();
            totalSales = totalSales.add(amount);
            if (order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().equals(today)) {
                todaySales = todaySales.add(amount);
                todayPaidOrders++;
            }
            if (recentOrders.size() < 5) {
                recentOrders.add(new OrderHistoryEntry(order.getOrderId(), order.getCreatedAt(),
                        order.getCustomerName(), order.getPhoneNumber(), order.getPaymentMethod(),
                        order.getPaymentStatus(), "NPR", order.getGrandTotal()));
            }
        }

        return new DashboardResponse(today, "NPR", totalSales, todaySales, paidOrders.size(),
                todayPaidOrders, orderRepository.countByPaymentStatus(PaymentStatus.PENDING),
                itemRepository.count(), categoryRepository.count(), recentOrders);
    }
}
