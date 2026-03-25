package sparta.paymentsystemserver.domain.refund.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.refund.entity.Refund;
import sparta.paymentsystemserver.domain.refund.entity.RefundStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.refund.repository.RefundRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;


// 환불 최종 반영 전용 서비스
// - 외부 포트원 취소 호출이 이미 성공한 뒤 DB 상태를 트랜잭션으로 반영
// - 환불 이력 저장
// - 포인트/재고/적립 취소 후처리를 RefundTransactionProcessor에 위임
// - payment/order 상태를 최종 REFUNDED 상태로 바꿈
@Service
@RequiredArgsConstructor
public class RefundFinalizeService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PublicIdGenerator publicIdGenerator;
    private final RefundTransactionProcessor refundTransactionProcessor;


    // 수동 환불 요청의 최종 반영을 수행함 이미 외부 결제 취소가 끝났다는 전제 하에 로컬 DB 상태만 반영
    @Transactional
    public PaymentResultResponse finalizeRefund(
            String paymentId,
            long refundAmount,
            String reason,
            String providerRefundId
    ) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 환불 완료된 결제라면 멱등하게 성공 응답을 반환
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return toResult(payment);
        }

        // PAID 상태인 결제만 환불 최종 반영 가능
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        // 이미 환불 이력이 있으면 중복 생성하지 않음
        if (!refundRepository.existsByPaymentId(payment.getId())) {
            Refund refund = Refund.create(
                    publicIdGenerator.generate("REF"),
                    payment,
                    refundAmount,
                    reason,
                    RefundStatus.COMPLETED,
                    providerRefundId
            );
            refundRepository.save(refund);
        }

        refundTransactionProcessor.processRefund(payment);
        payment.markRefunded();
        payment.getOrder().refund();

        return toResult(payment);
    }


    // 웹훅 기반 환불 동기화 최종 반영. 포트원에서 이미 취소/환불이 완료됐다는 사실을 웹훅으로 전달받았을 때 그 결과에 맞춰 동기화
    @Transactional
    public void finalizeRefundFromWebhook(
            String paymentId,
            String providerRefundId,
            String reason
    ) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (refundRepository.existsByPaymentId(payment.getId())) {
            payment.markRefunded();
            payment.getOrder().refund();
            return;
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            return;
        }

        Refund refund = Refund.create(
                publicIdGenerator.generate("REF"),
                payment,
                payment.getTotalAmount(),
                reason,
                RefundStatus.COMPLETED,
                providerRefundId
        );

        refundRepository.save(refund);
        refundTransactionProcessor.processRefund(payment);
        payment.markRefunded();
        payment.getOrder().refund();
    }

    // 환불 완료 응답 dto 생성
    private PaymentResultResponse toResult(Payment payment) {
        return new PaymentResultResponse(
                true,
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getStatus().name()
        );
    }
}
