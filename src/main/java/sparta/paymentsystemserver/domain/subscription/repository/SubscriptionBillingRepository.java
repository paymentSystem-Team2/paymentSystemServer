package sparta.paymentsystemserver.domain.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.subscription.entity.Subscription;
import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionBilling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 구독 청구 이력 조회용 레포지토리
public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, Long> {

    // 특정 구독의 청구 이력을 최근 순으로 조회함 사용자가 내역 볼 때 사용
    List<SubscriptionBilling> findAllBySubscriptionSubscriptionIdOrderByAttemptDateDesc(String subscriptionId);

    // billingId로 단건 조회
    Optional<SubscriptionBilling> findByBillingId(String billingId);

    // 같은 구독과 같은 청구 기간에 빌링 이력이 이미 존재하는지 확인함
    boolean existsBySubscriptionAndPeriodStartAndPeriodEnd(
            Subscription subscription,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );
}
