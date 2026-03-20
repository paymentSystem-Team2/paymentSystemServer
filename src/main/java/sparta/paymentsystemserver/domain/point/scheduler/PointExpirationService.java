package sparta.paymentsystemserver.domain.point.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.repository.PointRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointExpirationService {

    private final PointRepository pointRepository;
    private final UserService userService;
    private final PublicIdGenerator publicIdGenerator;

    @Transactional
    public void processExpiration(PointTransaction target, LocalDateTime now) {
        User user = userService.findById(target.getUser().getId());

//        차감될 포인트
        long points = Math.min(target.getPoints(), user.getPointBalance());

        if (points > 0) {
            PointTransaction pt = PointTransaction.expired(
                    publicIdGenerator.generate("PT"),
                    user,
                    points
            );

            pointRepository.save(pt);
            user.subtractPoint(points);
        }

        target.markExpired(now);

        log.info("[포인트 만료 스케쥴러] userId: {}, 소멸포인트: {}",
                user.getId(), points);
    }
}
