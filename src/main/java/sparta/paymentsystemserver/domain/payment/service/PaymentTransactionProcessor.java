package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;
import sparta.paymentsystemserver.domain.order.repository.OrderItemRepository;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.point.service.PointService;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 결제 성공 후에 내부 상태 반영이랑 실패하면 보상 처리를 담당
// PaymentService에서 결제 검증까지 끝내면 실제 주문 완료, 포인트 반영, 자동 취소 이후 보상은 얘가 맡음
@Component
@RequiredArgsConstructor
public class PaymentTransactionProcessor {

    private final PointService pointService;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;

    // 결제 성공 후 내부 후처리 결제 상태, 주문 상태, 포인트 사용/적립
    public void processSuccess(Payment payment, String providerTransactionId) {
        Order order = payment.getOrder();

        // 주문에 실제 사용 포인트와 할인 금액을 반영. 사용 포인트 전액이 할인 금액
        order.updatePoint(payment.getPointsToUse(), payment.getPointsToUse());

        // 결제 시 사용한 포인트를 실제 잔액에서 차감하고 사용 이력을 남김
        pointService.spendPoints(payment.getUser().getId(), order, payment.getPointsToUse());

        // 실제 외부 결제 금액 기준으로 적립 포인트를 계산하고 누적 결제 금액과 멤버십 등급도 함께 갱신
        // pointService.earnPoints(payment.getUser().getId(), order, payment.getExternalAmount());

        // 모든 내부 반영이 끝난 뒤 주문을 결제 완료 상태로 전환
        order.completePurchase();

        // 결제를 최종 승인 상태로 변경
        payment.markPaid(providerTransactionId);
    }

    // 포트원 검증 실패처럼 결제 최종 실패 처리해야 될 때 사용 별도 트랜잭션으로 실패 상태랑 확정해서 남김
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleVerificationFailure(String paymentId, String failureReason) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.markFailed(failureReason);
        payment.getOrder().cancel();
        restoreOrderStock(payment.getOrder());
    }

    // 결제 후처리 실패 뒤에 외부 결제 취소까지 성공한 경우 로컬 DB 상태를 보상 트랜잭션으로 정리함
    // 이 메서드는 외부 API 호출 없이 DB 상태 변경만 담당
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateAfterCancel(String paymentId, String failureReason) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.markFailed(failureReason);
        payment.getOrder().cancel();
        restoreOrderStock(payment.getOrder());
    }


    // 결제 후처리 실패 뒤 외부 결제 취소까지 실패한 경우 최소한의 결제 실패 이력은 별도 트랜잭션으로 남긴다
    // 외부 결제가 실제로 취소되지 않았을 수 있으니까 주문 취소나 재고 복구까지 단정해서 처리하지 않고
    // 우선 실패 상태와 사유만 남겨서 후속 대응이 가능하게 함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensationFailure(String paymentId, String failureReason) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.markFailed(failureReason);
    }

    // 주문 생성 했을 때 미리 차감했던 재고 돌림
    private void restoreOrderStock(Order order) {
        if (order.isStockRestored()) {
            return;
        }

        for (OrderItem orderItem : orderItemRepository.findWithProductByOrder(order)) {
            orderItem.getProduct().increaseStock(orderItem.getQuantity());
        }

        order.markStockRestored();
    }
}

