package sparta.paymentsystemserver.domain.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 주문 상품 총액
    @Column(nullable = false)
    private Long totalAmount;

    // 사용 포인트
    @Column(nullable = false)
    private Long usedPoints;

    // 포인트 할인 금액
    @Column(nullable = false)
    private Long pointDiscountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 주문 생성시간
    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column
    private LocalDateTime completedAt;

    public Order(String orderId, String orderNumber, User user, Long totalAmount, Long usedPoints, Long pointDiscountAmount, OrderStatus status, LocalDateTime orderedAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.user = user;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.pointDiscountAmount = pointDiscountAmount;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    // 결제 성공 시 > 주문 완료 상태로 변경
    public void complete() {
        this.status = OrderStatus.PAID;
        this.completedAt = LocalDateTime.now();
    }

    // 환불 발생 > 환불 상태로 변경
    public void refund() {
        this.status = OrderStatus.CANCELLED;
    }
}
