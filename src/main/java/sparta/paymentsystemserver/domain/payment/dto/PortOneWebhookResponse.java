package sparta.paymentsystemserver.domain.payment.dto;

// 포트원 웹훅 응답 dto
public record PortOneWebhookResponse(
        boolean success,
        String message
) {
}
