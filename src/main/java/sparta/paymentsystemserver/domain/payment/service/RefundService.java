package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;
import sparta.paymentsystemserver.domain.order.repository.OrderItemRepository;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.entity.Refund;
import sparta.paymentsystemserver.domain.payment.entity.RefundStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.payment.repository.RefundRepository;
import sparta.paymentsystemserver.domain.point.service.PointService;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

// 결제 환불 처리 서비스 환불 가능 여부 검증하고 바깥의 취소 요청과 내부 상태 변경 함께 처리함
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PublicIdGenerator publicIdGenerator;
    private final RefundTransactionProcessor refundTransactionProcessor;

    // 사용자 결제 환불 처리 외부 결제에서는 포트원에 취소를 요청하고 내부 결제(포인트 결제 등)에서는 즉시 환불 이력 생성
    @Transactional
    public PaymentResultResponse cancelPayment(String paymentId, Long userId, String reason) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        validateRefundRequest(payment, userId);

        // 이미 환불 완료된 결제는 성공 응답만 돌려줌
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return toResult(payment);
        }

        RefundExecution refundExecution = executeRefund(payment, reason);

        // 외부 환불이 성공한 뒤에는 내부 환불 이력을 남김
        Refund refund = Refund.create(
                publicIdGenerator.generate("REF"),
                payment,
                refundExecution.refundAmount(),
                reason,
                RefundStatus.COMPLETED,
                refundExecution.providerRefundId()
        );

        refundRepository.save(refund);

        // 환불 완료 후에 내부에 처리 수행
        refundTransactionProcessor.processRefund(payment);

        // 결제랑 주문 상태를 최종 환불 상태로 전환
        payment.markRefunded();
        payment.getOrder().refund();

        return toResult(payment);
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

    // 포트원 결제는 외부 취소 내부 결제는 내부 금액 기준으로 처리
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

    // 실제 환불 결과를 내부적으로 전달하기 위한 값들
    private record RefundExecution(
            String providerRefundId,
            long refundAmount
    ) {
    }
}
