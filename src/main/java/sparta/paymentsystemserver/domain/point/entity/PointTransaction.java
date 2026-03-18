package sparta.paymentsystemserver.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.global.config.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String pointTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

//    이번 거래 포인트 증감량
    @Column(nullable = false)
    private Long points;

//    이번 거래 후의 잔액 스냅샷
    @Column(nullable = false)
    private Long balanceSnapshot;

    private LocalDateTime expiresAt;
    private LocalDateTime expirationProcessedAt;

    public static PointTransaction earned(String id, User user, Order order, Long points, Long balanceSnapshot, LocalDateTime expiresAt) {
        PointTransaction pt = new PointTransaction();
        pt.pointTransactionId = id;
        pt.user = user;
        pt.order = order;
        pt.type = PointTransactionType.EARNED;
        pt.points = points;
        pt.balanceSnapshot = balanceSnapshot;
        pt.expiresAt = expiresAt;
        return pt;
    }
}