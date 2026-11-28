package in.dipesh.spring_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(LocalDate date, String currency, BigDecimal totalSales,
        BigDecimal todaySales, int paidOrders, int todayPaidOrders, long pendingPayments,
        long totalItems, long totalCategories, List<OrderHistoryEntry> recentOrders) { }
