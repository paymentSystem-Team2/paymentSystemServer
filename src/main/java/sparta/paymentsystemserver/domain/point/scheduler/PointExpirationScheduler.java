package sparta.paymentsystemserver.domain.point.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.repository.PointRepository;
import sparta.paymentsystemserver.domain.point.service.PointExpirationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpirationScheduler {

    private final PointRepository pointRepository;
    private final PointExpirationService pointExpirationService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 00시
    public void expirePoints() {
        log.info("[포인트 만료 스케쥴러] 실행 시작");

        LocalDateTime now = LocalDateTime.now();

        List<PointTransaction> targets = pointRepository
                .findExpiredEarnedPoints(now);

        if (targets.isEmpty()) {
            log.info("[포인트 만료 스케쥴러] 만료 대상 없음");
            return;
        }

        log.info("[포인트 만료 스케쥴러] 만료 대상 {}건 처리 시작", targets.size());

        for (PointTransaction target : targets) {
            try {
                pointExpirationService.processExpiration(target, now);
            } catch (Exception e) {
                log.error("[포인트 만료 스케쥴러] 실패 - pointTransactionId: {}, error: {}"
                        , target.getPointTransactionId(), e.getMessage());
            }
        }

        log.info("[포인트 만료 스케쥴러] 실행 완료");
    }
}
