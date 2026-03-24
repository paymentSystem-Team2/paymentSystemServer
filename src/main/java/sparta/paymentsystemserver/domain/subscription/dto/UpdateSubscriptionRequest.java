package sparta.paymentsystemserver.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;

// 구독 상태 변경 요청 dto
public record UpdateSubscriptionRequest(
        @NotBlank(message = "action은 필수입니다.")
        String action,
        String reason,
        String planId
) {
    // 액션 문자열을 비교하기 쉽게 정규화함
    public String normalizedAction() {
        return action == null ? "" : action.trim().toLowerCase();
    }

    // 구독 해지 요청인지 확인함
    public boolean isCancelAction() {
        return "cancel".equals(normalizedAction());
    }

    // 플랜 변경 요청인지 확인함
    public boolean isChangePlanAction() {
        return "change_plan".equals(normalizedAction()) || "change-plan".equals(normalizedAction());
    }

    // 구독 재가입
    public boolean isResumeAction() {
        return "resume".equals(normalizedAction()) || "keep".equals(normalizedAction());
    }
}
