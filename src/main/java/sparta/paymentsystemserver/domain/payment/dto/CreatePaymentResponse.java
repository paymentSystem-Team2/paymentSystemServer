package sparta.paymentsystemserver.domain.payment.dto;

// 결제 시도 생성 응답 dto
// 이 단계는 일단 결제 완료가 아니라 결제창을 열기 위한 준비 완료 상태를 반환
public record CreatePaymentResponse(
        String paymentId,
        String orderId,
        long totalAmount,
        long pointsToUse,
        long externalAmount,
        String status
) {
}
