package sparta.paymentsystemserver.domain.payment.dto;

// 웹훅 처리 결과 응답 dto
public record WebhookResponse(
        boolean success,
        String message
) {
}
