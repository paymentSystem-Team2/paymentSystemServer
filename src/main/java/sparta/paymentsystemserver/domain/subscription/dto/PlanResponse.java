package sparta.paymentsystemserver.domain.subscription.dto;

import sparta.paymentsystemserver.domain.subscription.entity.Plan;

// 프론트에 보낼 플랜 조회 응답 dto
public record PlanResponse(
        String planId,
        String name,
        Long amount,
        String billingCycle,
        String description
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getPlanId(),
                plan.getName(),
                plan.getAmount(),
                plan.getBillingCycle().name(),
                plan.getDescription()
        );
    }
}
