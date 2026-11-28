package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.CategoryRepository;
import in.dipesh.spring_backend.repository.ItemRepository;
import in.dipesh.spring_backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final ItemRepository items = mock(ItemRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final DashboardService dashboard = new DashboardService(orders, items, categories);
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

    @Test
    void calculatesExactPaidSalesAndSeparatesTodaysOrders() {
        var current = paidOrder("ORD-today", "10.10", today);
        var previous = paidOrder("ORD-previous", "20.20", today.minusDays(1));
        when(orders.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID))
                .thenReturn(List.of(current, previous));
        when(orders.countByPaymentStatus(PaymentStatus.PENDING)).thenReturn(3L);
        when(items.count()).thenReturn(8L);
        when(categories.count()).thenReturn(2L);

        var summary = dashboard.getSummary();
        assertEquals(today, summary.date());
        assertEquals("NPR", summary.currency());
        assertEquals(new BigDecimal("30.30"), summary.totalSales());
        assertEquals(new BigDecimal("10.10"), summary.todaySales());
        assertEquals(2, summary.paidOrders());
        assertEquals(1, summary.todayPaidOrders());
        assertEquals(3L, summary.pendingPayments());
        assertEquals(8L, summary.totalItems());
        assertEquals(2L, summary.totalCategories());
        assertEquals("ORD-today", summary.recentOrders().get(0).orderId());
        verify(orders).findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID);
        verify(orders, never()).save(any());
    }

    @Test
    void limitsRecentOrdersToFiveButCountsAllPaidSales() {
        var paid = new ArrayList<OrderEntity>();
        for (int i = 0; i < 7; i++) paid.add(paidOrder("ORD-" + i, "1.00", today.minusDays(i)));
        when(orders.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID)).thenReturn(paid);
        var summary = dashboard.getSummary();
        assertEquals(7, summary.paidOrders());
        assertEquals(new BigDecimal("7.00"), summary.totalSales());
        assertEquals(5, summary.recentOrders().size());
        assertEquals("ORD-4", summary.recentOrders().get(4).orderId());
    }

    @Test
    void emptyDatabaseShowsZeroTotalsAndNoRecentOrders() {
        when(orders.findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PAID)).thenReturn(List.of());
        var summary = dashboard.getSummary();
        assertEquals(BigDecimal.ZERO, summary.totalSales());
        assertEquals(BigDecimal.ZERO, summary.todaySales());
        assertEquals(0, summary.paidOrders());
        assertEquals(0, summary.todayPaidOrders());
        assertTrue(summary.recentOrders().isEmpty());
    }

    private OrderEntity paidOrder(String id, String total, LocalDate date) {
        return OrderEntity.builder().orderId(id).grandTotal(new BigDecimal(total))
                .createdAt(date.atTime(12, 0)).customerName("Customer")
                .paymentMethod(PaymentMethod.CASH).paymentStatus(PaymentStatus.PAID).build();
    }
}
