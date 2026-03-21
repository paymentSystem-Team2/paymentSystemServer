package sparta.paymentsystemserver.domain.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradePolicy;
import sparta.paymentsystemserver.domain.membership.repository.MembershipGradeRepository;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.point.dto.PointHistoryResponse;
import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.entity.PointTransactionType;
import sparta.paymentsystemserver.domain.point.repository.PointRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;
    private final UserService userService;
    private final PublicIdGenerator publicIdGenerator;

//    포인트 거래 내역 조회
    public List<PointHistoryResponse> getPointHistory(Long userId) {
        User user = userService.findById(userId);
        return pointRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(PointHistoryResponse::from)
                .toList();
    }

//    구매확정 시 포인트 적립
    @Transactional
    public void earnPoints(Long userId, Order order, Long paymentAmount) {
        User user = userService.findById(userId);

        MembershipGradePolicy membershipGradePolicy = userService.getUserMemberShip(user);

        long points = BigDecimal.valueOf(paymentAmount)
                .multiply(membershipGradePolicy.getEarnRate())
                .longValue();

        if (points > 0) {
            PointTransaction pt = PointTransaction.earned(
                    publicIdGenerator.generate("PT"),
                    user,
                    order,
                    points,
                    LocalDateTime.now().plusYears(1)
            );

            pointRepository.save(pt);

            user.addPoint(points);
        }

        user.addTotalPaidAmount(paymentAmount);
        userService.calculateGrade(user);

        log.info("[포인트 적립] userId: {}, 결제금액: {}, 적립포인트: {}, 등급: {}",
                userId, paymentAmount, points, user.getMembershipGrade());
    }

//    결제 확정 시 포인트 사용
    @Transactional
    public void spendPoints(Long userId, Order order, Long usePoints) {
        if (usePoints == null || usePoints <= 0) return;

        User user = userService.findById(userId);

        user.subtractPoint(usePoints);

        PointTransaction pt = PointTransaction.spend(
                publicIdGenerator.generate("PT"),
                user,
                order,
                usePoints
        );

        pointRepository.save(pt);

        log.info("[포인트 사용] userId: {}, 사용포인트: {}",
                userId, usePoints);
    }

//    환불 시 포인트 복구
    @Transactional
    public void restorePoints(Long userId, Order order) {
        User user = userService.findById(userId);

//        해당 결제에서 사용한 포인트가 없는 경우 그냥 반환
        PointTransaction target = pointRepository
                .findByOrderAndType(order, PointTransactionType.SPENT)
                .orElse(null);

        if (target == null) return;

        PointTransaction pt = PointTransaction.restored(
                publicIdGenerator.generate("PT"),
                user,
                order,
                target.getPoints()
        );

        pointRepository.save(pt);
        user.addPoint(pt.getPoints());

        log.info("[포인트 복구] userId: {}, 복구포인트: {}",
                userId, target.getPoints());
    }

//    환불 시 적립 포인트 취소, 누적 결제금액 감소, 등급 재계산 수행
    @Transactional
    public void cancelEarnedPoints(Long userId, Order order, Long paymentAmount) {
        User user = userService.findById(userId);

//        해당 결제에서 사용한 포인트가 없는 경우
        PointTransaction target = pointRepository
                .findByOrderAndType(order, PointTransactionType.EARNED)
                .orElse(null);

        if (target != null) {
            user.subtractPoint(target.getPoints());

            PointTransaction pt = PointTransaction.earnCanceled(
                    publicIdGenerator.generate("PT"),
                    user,
                    order,
                    target.getPoints()
            );

            pointRepository.save(pt);

            log.info("[적립 취소] userId: {}, 취소 포인트: {}",
                    userId, target.getPoints());
        }

        user.subtractTotalPaidAmount(paymentAmount);
        userService.calculateGrade(user);

        log.info("[환불 후 등급 재계산] userId: {}, 누적결제금액: {}, 등급: {}",
                userId, user.getTotalPaidAmount(), user.getMembershipGrade());
    }
}
