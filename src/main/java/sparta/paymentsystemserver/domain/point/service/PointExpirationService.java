package sparta.paymentsystemserver.domain.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.repository.PointRepository;
import sparta.paymentsystemserver.domain.point.repository.PointUsageDetailRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointExpirationService {

    private final PointRepository pointRepository;
    private final PointUsageDetailRepository pointUsageDetailRepository;
    private final UserService userService;
    private final PublicIdGenerator publicIdGenerator;

    @Transactional
    public void processExpiration(PointTransaction target, LocalDateTime now) {
        User user = userService.findById(target.getUser().getId());

//        이미 사용된 포인트 조회
        long alreadyUsed = pointUsageDetailRepository.sumUsedPointsByEarned(target);

        long remainPoint = target.getPoints() - alreadyUsed;

        long pointsToExpire = Math.min(remainPoint, user.getPointBalance());

        if (pointsToExpire > 0) {
            PointTransaction pt = PointTransaction.expired(
                    publicIdGenerator.generate("PT"),
                    user,
                    pointsToExpire
            );
            pointRepository.save(pt);
            user.subtractPoint(pointsToExpire);
        }

        target.markExpired(now);

        log.info("[포인트 만료 스케쥴러] userId: {}, 소멸포인트: {}",
                user.getId(), pointsToExpire);
    }
}
