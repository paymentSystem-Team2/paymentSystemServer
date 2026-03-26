package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 결제 최종 반영 전용 서비스
// 외부 결제 상태 검증이 끝난 뒤에 payment를 비관적 락으로 다시 조회
// READY 상태인 결제만 최종 확정하고 이미 PAID 상태인 경우 멱등하게 성공 처리한다
// PaymentService에서 외부 포트원 조회를 끝낸 뒤에 이 서비스를 호출하도록 분리해서 디비 락을 잡은 상태로 외부 api 응답을 기다리지 않도록 함
@Service
@RequiredArgsConstructor
public class PaymentFinalizeService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionProcessor paymentTransactionProcessor;

    // 내부 결제를 최종 확정함. 내부 결제 외부 PG 조회가 필요 없으니까 짧은 트랜잭션 안에서 payment 상태와 주문 상태만 반영
    @Transactional
    public PaymentResultResponse finalizeInternalPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return toResult(payment);
        }

        if (payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentException(ErrorCode.PAYMENT_CONFIRM_NOT_ALLOWED);
        }

        paymentTransactionProcessor.processSuccess(payment, "INTERNAL");
        return toResult(payment);
    }

    // 포트원 결제를 최종 확정함
    // 외부 결제 상태와 금액 검증은 이미 끝난 상태라고 가정하고 여기서는 최종 상태 반영만 처리
    // 이 메서드는 보상 처리까지 수행하지 않음 최종 반영 실패하면 예외만 던지고 실제 외부 취소와 보상은 PaymentService에서 처리
    @Transactional
    public PaymentResultResponse finalizePortOnePayment(String paymentId, String providerTransactionId) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return toResult(payment);
        }

        if (payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentException(ErrorCode.PAYMENT_CONFIRM_NOT_ALLOWED);
        }

        paymentTransactionProcessor.processSuccess(payment, providerTransactionId);
        return toResult(payment);
    }


    // 결제 성공 응답 dto 생성
    private PaymentResultResponse toResult(Payment payment) {
        return new PaymentResultResponse(
                true,
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getStatus().name()
        );
    }
}