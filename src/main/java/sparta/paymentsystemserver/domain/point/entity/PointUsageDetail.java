package sparta.paymentsystemserver.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import sparta.paymentsystemserver.global.config.BaseEntity;

@Getter
@Entity
@Table(name = "point_usage_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUsageDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    어느 SPENT에서 발생했는지 확인 용도
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spent_transaction_id", nullable = false)
    private PointTransaction spentTransaction;

    //    어느 EARNED에서 발생했는지 확인 용도
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "earned_transaction_id", nullable = false)
    private PointTransaction earnedTransaction;

    //    실제로 차감된 포인트
    @Column(nullable = false)
    private Long usedPoints;

    public static PointUsageDetail of(PointTransaction spent, PointTransaction earned, Long usedPoints) {
        PointUsageDetail detail = new PointUsageDetail();
        detail.spentTransaction = spent;
        detail.earnedTransaction = earned;
        detail.usedPoints = usedPoints;
        return detail;
    }
}

