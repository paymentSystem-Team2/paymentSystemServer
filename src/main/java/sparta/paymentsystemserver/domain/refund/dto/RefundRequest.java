package sparta.paymentsystemserver.domain.refund.dto;

// 전액 환불 요청 dto
public record RefundRequest(
        // 환불 요청 사유
        String reason
) {
}
