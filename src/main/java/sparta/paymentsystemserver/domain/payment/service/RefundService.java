package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.*;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.payment.repository.RefundRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;


@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PublicIdGenerator publicIdGenerator;

    @Transactional
    public PaymentResultResponse cancelPayment(String paymentId, Long userId, String reason) {

        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제만 환불 가능
        if (!payment.getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 이미 환불된 경우 멱등 처리
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return new PaymentResultResponse(
                    true,
                    payment.getPaymentId(),
                    payment.getOrder().getOrderId(),
                    payment.getStatus().name()
            );
        }

        // 결제 완료 상태만 환불 가능
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        // 동일 결제 환불 이력 중복 방지
        if (refundRepository.existsByPaymentId(payment.getId())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }

        String providerRefundId = "INTERNAL";
        long refundAmount = payment.getExternalAmount();

        if (payment.getProvider() == PaymentProvider.PORTONE) {
            PortOnePaymentClient.PortOneCancelInfo cancelInfo =
                    portOnePaymentClient.cancelPayment(payment.getPaymentId(), reason);

            if (!cancelInfo.isCancelled()) {
                throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
            }

            providerRefundId = cancelInfo.cancellationId();
            refundAmount = cancelInfo.cancelledAmount();
        }

        providerRefundId = cancelInfo.cancellationId();
        refundAmount = cancelInfo.cancelledAmount();

        // 환불은 단순 상태 변경이 아니라서 별도 도메인 이력으로 관리한다
        Refund refund = Refund.create(
                publicIdGenerator.generate("REF"),
                payment,
                refundAmount,
                reason,
                RefundStatus.COMPLETED,
                providerRefundId
        );

        refundRepository.save(refund);

        // 외부 환불이 정상적으로 완료되면 내부 결제 상태와 주문상태도 함께 REFUNDED로 변경함
        payment.markRefunded();
        payment.getOrder().refund();

        return new PaymentResultResponse(
                true,
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getStatus().name()
        );
    }
}
