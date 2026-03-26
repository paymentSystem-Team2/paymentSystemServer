package sparta.paymentsystemserver.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.subscription.dto.BillingResponse;
import sparta.paymentsystemserver.domain.subscription.entity.Subscription;
import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionStatus;
import sparta.paymentsystemserver.domain.subscription.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

// 구독 자동 정기 청구와 예약 해지 완료 처리를 담당하는 스케줄러
// ACTIVE 상태면서 nextBillingAt이 지난 구독을 찾아서 자동 청구함
// 실행 주기는 매시간마다 1회 실행
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingService billingService;

    // 구독 스케줄러 메인 실행 메서드 매시간 0분 0초에 실행함
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "subscriptionScheduler_runSubscriptionScheduler", lockAtMostFor = "PT50M", lockAtLeastFor = "PT10S")
    @Transactional
    public void runSubscriptionScheduler() {
        log.info("[구독 스케줄러] 실행 시작");

        LocalDateTime now = LocalDateTime.now();

        processScheduledBillings(now);
        processScheduledCancellations(now);

        log.info("[구독 스케줄러] 실행 완료");
    }

    // 자동 청구 대상 구독을 조회해서 청구를 실행함
    protected void processScheduledBillings(LocalDateTime now) {
        List<Subscription> targets = subscriptionRepository
                .findAllByStatusAndNextBillingAtBeforeAndCancelAtPeriodEndFalse(
                        SubscriptionStatus.ACTIVE,
                        now
                );

        if (targets.isEmpty()) {
            log.info("[구독 스케줄러] 자동 청구 대상 없음");
            return;
        }

        log.info("[구독 스케줄러] 자동 청구 대상 {}건 처리 시작", targets.size());

        for (Subscription subscription : targets) {
            try {
                BillingResponse result = billingService.createScheduledBilling(subscription);

                log.info("[구독 스케줄러] 자동 청구 처리 완료 - subscriptionId={}, billingStatus={}",
                        subscription.getSubscriptionId(),
                        result.status());
            } catch (Exception exception) {
                log.error("[구독 스케줄러] 자동 청구 처리 실패 - subscriptionId={}, error={}",
                        subscription.getSubscriptionId(),
                        exception.getMessage());
            }
        }
    }

    // 예약 해지된 구독 중에 현재 이용 기간이 끝난 구독을 최종 종료 처리함
    protected void processScheduledCancellations(LocalDateTime now) {
        List<Subscription> targets = subscriptionRepository
                .findAllByCancelAtPeriodEndTrueAndCurrentPeriodEndBefore(now);

        if (targets.isEmpty()) {
            log.info("[구독 스케줄러] 해지 완료 대상 없음");
            return;
        }

        log.info("[구독 스케줄러] 해지 완료 대상 {}건 처리 시작", targets.size());

        for (Subscription subscription : targets) {
            subscription.completeCancellation();

            log.info("[구독 스케줄러] 해지 완료 처리 - subscriptionId={}",
                    subscription.getSubscriptionId());
        }
    }
}