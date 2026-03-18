package sparta.paymentsystemserver.domain.payment.dto;

// 결제 확정이랑 환불 응답 dto
// 프론트에서 이 응답으로 결제 확정 성공 여부랑 주문/결제 상태 확인
public record PaymentResultResponse(
        boolean success,
        String paymentId,
        String orderId,
        String status
) {
}
