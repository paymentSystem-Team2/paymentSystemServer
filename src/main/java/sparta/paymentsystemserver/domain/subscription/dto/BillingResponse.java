package sparta.paymentsystemserver.domain.subscription.dto;

import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionBilling;

import java.time.LocalDateTime;

// 청구 실행 결과 응답 dto
public record BillingResponse(
        String BillingId,
        String paymentId,
        Long amount,
        String status,
        LocalDateTime attemptDate,
        String failureMessage
) {
    public static BillingResponse from(SubscriptionBilling billing) {
        return new BillingResponse(
                billing.getBillingId(),
                billing.getPaymentId(),
                billing.getAmount(),
                billing.getStatus().name(),
                billing.getAttemptDate(),
                billing.getFailureMessage()
        );
    }
}
