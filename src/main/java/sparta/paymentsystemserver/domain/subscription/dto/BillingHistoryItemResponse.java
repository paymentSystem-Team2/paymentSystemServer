package sparta.paymentsystemserver.domain.subscription.dto;

import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionBilling;

import java.time.LocalDateTime;

// 청구 이력 단건 응답 dto
public record BillingHistoryItemResponse(
        String billingId,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        Long amount,
        String status,
        String paymentId,
        LocalDateTime attemptDate,
        String failureMessage
) {
    public static BillingHistoryItemResponse from(SubscriptionBilling billing) {
        return new BillingHistoryItemResponse(
                billing.getBillingId(),
                billing.getPeriodStart(),
                billing.getPeriodEnd(),
                billing.getAmount(),
                billing.getStatus().name(),
                billing.getPaymentId(),
                billing.getAttemptDate(),
                billing.getFailureMessage()
        );
    }
}
