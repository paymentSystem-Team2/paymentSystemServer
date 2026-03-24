package sparta.paymentsystemserver.domain.subscription.entity;

// 구독의 현재 상태를 나타냄
public enum SubscriptionStatus {

    // 구독 생성은 됐지만 첫 청구가 아직 완료되지 않은 상태
    PENDING,
    // 현재 정상적으로 구독 중인 상태
    ACTIVE,
    // 사용자가 해지
    CANCELLED,
    // 미납, 연속 실패로 정지. 재구독 청구 가능
    SUSPENDED,
    // 기간 종료
    EXPIRED
}
