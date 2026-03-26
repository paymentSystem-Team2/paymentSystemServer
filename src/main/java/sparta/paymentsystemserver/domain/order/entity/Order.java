package sparta.paymentsystemserver.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.domain.order.exception.OrderInvalidStatusException;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.time.LocalDateTime;

import static sparta.paymentsystemserver.global.exception.ErrorCode.ORDER_STATUS_CHANGE_NOT_ALLOWED;

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
    private Long usedPoints = 0L;

    // 포인트 할인 금액
    @Column(nullable = false)
    private Long pointDiscountAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 주문 생성시간
    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime purchasedAt;

    @Column(nullable = false)
    private boolean stockRestored = false;

    public Order(String orderId, String orderNumber, User user, Long totalAmount, OrderStatus status, LocalDateTime orderedAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.user = user;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.pointDiscountAmount = pointDiscountAmount;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    public void updatePoint(Long usedPoints, Long pointDiscountAmount){
        this.usedPoints = usedPoints;
        this.pointDiscountAmount = pointDiscountAmount;
    }

    // 결제 성공 시 > 주문 배송중 상태로 변경
    public void completePurchase() {
        checkIsConfirmed();
        this.status = OrderStatus.PAID;
        this.purchasedAt = LocalDateTime.now();
    }

    public void purchaseConfirmed() {
        checkIsConfirmed();
        this.status = OrderStatus.PURCHASE_CONFIRMED;
        this.completedAt = LocalDateTime.now();
    }

    public void processDelivery() {
        checkIsConfirmed();

        switch (status) {
            case PAID:
                this.status = OrderStatus.SHIPPED;
                break;
            case SHIPPED:
                this.status = OrderStatus.DELIVERED;
                break;
            default:
                throw new OrderInvalidStatusException(ORDER_STATUS_CHANGE_NOT_ALLOWED);
        }
    }

    // 환불 발생 > 환불 상태로 변경
    public void refund() {
        checkIsConfirmed();
        this.status = OrderStatus.CANCELLED;
    }

    // 결제 실패나 내부에서 주문을 더 이상 진행하지 않을 때 취소 상태로 전환
    public void cancel() {
        checkIsConfirmed();
        this.status = OrderStatus.CANCELLED;
    }

    private void checkIsConfirmed(){
        if(isConfirmed()){
            throw new OrderInvalidStatusException(ORDER_STATUS_CHANGE_NOT_ALLOWED);
        }
    }

    public boolean isConfirmed(){
        return this.status.equals(OrderStatus.PURCHASE_CONFIRMED);
    }

    public boolean isStockRestored() {
        return stockRestored;
    }

    public void markStockRestored() {
        this.stockRestored = true;
    }
}
