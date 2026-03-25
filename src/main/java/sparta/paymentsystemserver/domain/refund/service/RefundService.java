package sparta.paymentsystemserver.domain.refund.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.refund.repository.RefundRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 결제 환불 처리 서비스 환불 가능 여부 검증하고 바깥의 취소 요청과 내부 상태 변경 함께 처리함
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PortOnePaymentClient portOnePaymentClient;
    private final RefundFinalizeService refundFinalizeService;

    // 사용자의 수동 환불 요청 진입 메서드
    // 1. payment를 조회해서 권한과 상태를 검증
    // 2. 포트원 결제인 경우에 외부 취소 api를 먼저 호출
    // 3. 외부 취소가 끝난 뒤 RefundFinalizeService에서 DB 상태 반영
    public PaymentResultResponse cancelPayment(String paymentId, Long userId, String reason) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        validateRefundRequest(payment, userId);

        if (payment.getOrder().isConfirmed()) {
            return toFailResult(payment);
        }

        // 이미 환불 완료된 결제라면 멱등하게 성공 응답을 반환
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return toResult(payment);
        }

        RefundExecution refundExecution = executeRefund(payment, reason);

        return refundFinalizeService.finalizeRefund(
                payment.getPaymentId(),
                refundExecution.refundAmount(),
                reason,
                refundExecution.providerRefundId()
        );
    }

    // 포트원 웹훅으로 이미 취소/환불된 결제를 동기화함
    // 이 메서드는 외부 api를 다시 호출하지 않고 포트원이 보내준 결과를 기준으로 환불 상태만 맞춤
    public void syncRefundFromWebhook(String paymentId, String providerRefundId, String reason) {
        refundFinalizeService.finalizeRefundFromWebhook(paymentId, providerRefundId, reason);

        log.info("[RefundSync] 웹훅 기준 환불 동기화를 완료했습니다. paymentId={}, providerRefundId={}", paymentId, providerRefundId);
    }

    // 환불 요청 유효한지 검증. 본인 결제 여부랑 결제 상태랑 중복 환불 여부를 확인함
    private void validateRefundRequest(Payment payment, Long userId) {
        if (!payment.getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        if (refundRepository.existsByPaymentId(payment.getId())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
    }

    // 외부 결제 취소를 수행하고 환불 결과를 반환
    // 내부 결제는 외부 호출이 필요 없으니까 전체 금액 기준 결과를 바로 만든다
    // 포트원 결제는 취소 api 호출해서 실제 취소 결과를 확인한다
    private RefundExecution executeRefund(Payment payment, String reason) {
        if (payment.getProvider() != PaymentProvider.PORTONE) {
            // 실제 주문 기준 환불 금액을 이력에 남김
            return new RefundExecution("INTERNAL", payment.getTotalAmount());
        }

        PortOneCancelInfo cancelInfo = portOnePaymentClient.cancelPayment(payment.getPaymentId(), reason);

        if (!cancelInfo.isCancelled()) {
            throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
        }

        return new RefundExecution(cancelInfo.cancellationId(), cancelInfo.cancelledAmount());
    }

    // 환불 완료 후에 프론트에 반환할 공통 응답
    private PaymentResultResponse toResult(Payment payment) {
        return new PaymentResultResponse(
                true,
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getStatus().name()
        );
    }

    private PaymentResultResponse toFailResult(Payment payment) {
        return new PaymentResultResponse(
                false,
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getStatus().name()
        );
    }

    // 실제 환불 결과를 내부적으로 전달하기 위한 값들
    private record RefundExecution(
            String providerRefundId,
            long refundAmount
    ) {
    }
}