package sparta.paymentsystemserver.domain.payment.dto;

// 전액 환불 요청 dto
public record CancelPaymentRequest(
        // 환불 요청 사유
        String reason
) {
}
