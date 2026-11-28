package in.dipesh.spring_backend.repository;

import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    long countByPaymentStatus(PaymentStatus status);
    java.util.List<OrderEntity> findByPaymentStatusOrderByCreatedAtDescIdDesc(PaymentStatus status);
    // Shared by all payment providers. Use MySQL/MariaDB-compatible locking SQL:
    // MySQL JDBC dialect detection can otherwise emit FOR UPDATE OF <alias>,
    // which MariaDB does not support. Callers must hold a transaction.
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT * FROM tbl_orders WHERE order_id = :orderId FOR UPDATE",
            nativeQuery = true)
    Optional<OrderEntity> findByOrderId(@org.springframework.data.repository.query.Param("orderId") String orderId);
    Optional<OrderEntity> findByKhaltiPidx(String khaltiPidx);
}
