package sparta.paymentsystemserver.domain.payment.entity;

// 결제 상태는 외부 결제 시도랑 내부 확정 단계를 함께 표현
public enum PaymentStatus {
    // 결제 시도만 생성되고 아직 확정되지 않은 상태
    READY,
    // 결제가 최종 승인된 상태
    PAID,
    // 결제 검증 또는 처리에 실패한 상태
    FAILED,
    // 전액 환불 완료!
    REFUNDED
}
