package in.dipesh.spring_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "tbl_orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private String customerName;
    private String phoneNumber;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING) //It tells Hibernate how to store the enum in the database.
    @Builder.Default //stop Lombok's builder from wiping out your = UNPAID default.
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) //Cascade = "whatever happens to the parent, do the same to the children." e.g orderRepository.save(order) saves order AND all its items automatically ✅
                                                                //"Orphan" = a child that no longer belongs to any parent.
    @JoinColumn(name = "order_id") //Hey, create a column called order_id in the items table so each item remembers which order it belongs to.
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();
    /*
    One order has many items. The items table has an order_id column linking back. When I save the order, save all its items too.
    When I remove an item from the list, delete it from the DB.
    */

    @PrePersist
    //@PrePersist is a JPA lifecycle callback.
    // It runs automatically right before the entity is first inserted into the database (i.e., before the INSERT runs).
    protected void onCreate(){
        this.orderId = "ORD"+System.currentTimeMillis();
        this.createdAt = LocalDateTime.now();
    }

    /*
        Why protected? JPA/Hibernate calls it via reflection/proxying.
        It doesn't need to be public — it's not meant to be called by your code.
        protected (or package-private) keeps it hidden from outside callers while still being callable by the persistence provider.
        This is the conventional signature.
    */
}
