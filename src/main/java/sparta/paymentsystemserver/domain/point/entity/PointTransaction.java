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

//    거래 포인트 증감량
    @Column(nullable = false)
    private Long points;

    private LocalDateTime expiresAt;
    private LocalDateTime expirationProcessedAt;

//    포인트 적립
    public static PointTransaction earned(String id, User user, Order order, Long points, LocalDateTime expiresAt) {
        PointTransaction pt = new PointTransaction();
        pt.pointTransactionId = id;
        pt.user = user;
        pt.order = order;
        pt.type = PointTransactionType.EARNED;
        pt.points = points;
        pt.expiresAt = expiresAt;
        return pt;
    }

//    포인트 사용
    public static PointTransaction spend(String id, User user, Order order, Long points) {
        PointTransaction pt = new PointTransaction();
        pt.pointTransactionId = id;
        pt.user = user;
        pt.order = order;
        pt.type = PointTransactionType.SPENT;
        pt.points = points;
        return pt;
    }

//    환불 시 포인트 복구
    public static PointTransaction restored(String id, User user, Order order, Long points) {
        PointTransaction pt = new PointTransaction();
        pt.pointTransactionId = id;
        pt.user = user;
        pt.order = order;
        pt.type = PointTransactionType.RESTORED;
        pt.points = points;
        return pt;
    }

//    환불 시 적립 포인트 취소
    public static PointTransaction earnCanceled(String id, User user, Order order, Long points) {
        PointTransaction pt = new PointTransaction();
        pt.pointTransactionId = id;
        pt.user = user;
        pt.order = order;
        pt.type = PointTransactionType.EARN_CANCELLED;
        pt.points = points;
        return pt;
    }

//    스케쥴러 만료 처리
    public static PointTransaction expired(String id, User user, Long points) {
        PointTransaction pt = new PointTransaction();
        pt.pointTransactionId = id;
        pt.user = user;
        pt.type = PointTransactionType.EXPIRED;
        pt.points = points;
        return pt;
    }

//    소멸 기록
    public void markExpired(LocalDateTime processedAt) {
        this.expirationProcessedAt = processedAt;
    }
}