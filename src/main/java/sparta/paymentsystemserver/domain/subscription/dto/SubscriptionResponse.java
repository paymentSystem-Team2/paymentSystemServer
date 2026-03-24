package sparta.paymentsystemserver.domain.subscription.dto;

import sparta.paymentsystemserver.domain.subscription.entity.Subscription;

import java.time.LocalDateTime;

// 구독 생성/조회/수정 후 공통 응답 dto
// 프론트 구독 관리 화면에서 필요한 값들을 한 번에 내려줌
public record SubscriptionResponse(
        String subscriptionId,
        String customerUid,
        String planId,
        String pendingPlanId,
        String paymentMethodId,
        String status,
        Boolean cancelAtPeriodEnd,
        Long amount,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        LocalDateTime nextBillingAt
) {

    // Subscription 엔티티를 응답 dto로 변환
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getSubscriptionId(),
                subscription.getPaymentMethod().getCustomerUid(),
                subscription.getPlan().getPlanId(),
                subscription.getPendingPlan() != null ? subscription.getPendingPlan().getPlanId() : null,
                String.valueOf(subscription.getPaymentMethod().getId()),
                subscription.getStatus().name(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getAmount(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getNextBillingAt()
        );
    }
}