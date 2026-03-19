package sparta.paymentsystemserver.domain.payment.dto;

// 포트원 웹훅 요청 dto
// 웹훅 페이로드는 최종 신뢰 대상이 아니니까 최소 필드만 받음 실제 최종 결제 상태는 서버가 paymentId로 포트원 재조회 후에 판단
public record PortOneWebhookRequest(
        String paymentId,
        String status
) {
}
