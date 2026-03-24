package sparta.paymentsystemserver.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 구독 생성 요청 dto
// 프론트는 포트원SDK를 통해서 빌링키를 먼저 발급받고 그 결과를 이 요청으로 백엔드에 전달합니다
// 이 요청은 실제 과금을 수행하는 것이 아니라 결제수단 등록이랑 구독 생성만 담당합니다
public record CreateSubscriptionRequest(
        @NotBlank(message = "customerUid는 필수입니다.")
        String customerUid,

        @NotBlank(message = "planId는 필수입니다.")
        String planId,

        @NotBlank(message = "billingKey는 필수입니다.")
        String billingKey,

        @NotNull(message = "amount는 필수입니다.")
        @Positive(message = "amount는 0보다 커야 합니다.")
        Long amount
) {
}