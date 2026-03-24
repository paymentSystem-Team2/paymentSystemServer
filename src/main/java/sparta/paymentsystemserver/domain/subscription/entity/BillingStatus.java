package sparta.paymentsystemserver.domain.subscription.entity;

// 구독 청구 이력의 상태
public enum BillingStatus {
    // 청구 생성 직후, 아직 결과 확정 전
    PENDING,
    // 결제 성공
    COMPLETED,
    // 결제 실패
    FAILED
}
