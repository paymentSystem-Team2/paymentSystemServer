package sparta.paymentsystemserver.domain.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sparta.paymentsystemserver.domain.subscription.entity.Subscription;
import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 구독 조회하는 레포지토리
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 특정 사용자의 현재 활성 구독 조회
    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    // 자동 정기 청구 대상 구독을 조회함
    // ACTIVE상태인지 nextBillingAt이랑 cancelAtPeriodEnd(해지 예약)이 걸려 있지 않은 구독
    List<Subscription> findAllByStatusAndNextBillingAtBeforeAndCancelAtPeriodEndFalse(
            SubscriptionStatus status,
            LocalDateTime nextBillingAt
    );

    // 예약 해지된 구독 중에 현재 이용 기간이 끝난 구독을 조회함
    List<Subscription> findAllByCancelAtPeriodEndTrueAndCurrentPeriodEndBefore(LocalDateTime currentPeriodEnd);

    // 특정 사용자의 구독 1건 조회
    Optional<Subscription> findBySubscriptionIdAndUserId(String subscriptionId, Long userId);

    // 구독 상세 조회 할 때 plan이랑 paymentMethod를 함께 조회
    @Query("""
            select s
            from Subscription s
            join fetch s.plan p
            join fetch s.paymentMethod pm
            where s.subscriptionId = :subscriptionId
            and s.user.id = :userId
            """)
    Optional<Subscription> findDetailBySubscriptionIdAndUserId(
            @Param("subscriptionId") String subscriptionId,
            @Param("userId") Long userId
    );
}
