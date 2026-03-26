package sparta.paymentsystemserver.domain.point.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.point.service.PointService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseConfirmScheduler {

    private final OrderRepository orderRepository;
    private final PointService pointService;

    private static final long CONFIRM_AFTER_DAYS = 7L;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 00시
    @SchedulerLock(name = "purchaseConfirmScheduler_confirmPurchase", lockAtMostFor = "PT30M", lockAtLeastFor = "PT10S")
    @Transactional
    public void confirmPurchase() {
        log.info("[구매확정 스케쥴러] 실행 시작");

        LocalDateTime threshold = LocalDateTime.now().minusDays(CONFIRM_AFTER_DAYS);

//        결제 완료된 지 7일이 넘은 주문들 조회
        List<Order> targets = orderRepository
                .findByStatusAndPurchasedAtBefore(OrderStatus.PAID, threshold);

        if (targets.isEmpty()) {
            log.info("[구매확정 스케쥴러] 대상 없음");
            return;
        }

        log.info("[구매확정 스케쥴러] 대상 {}건 처리 시작", targets.size());

        for (Order order : targets) {
            try {
                order.purchaseConfirmed();
                pointService.earnPoints(
                        order.getUser(),
                        order,
                        order.getTotalAmount()
                );
            } catch (Exception e) {
                log.error("[구매확정 스케쥴러] 실패 - orderId: {}, error: {}",
                        order.getOrderId(), e.getMessage());
            }
        }

        log.info("[구매확정 스케쥴러] 실행 완료");
    }
}
