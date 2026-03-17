package sparta.paymentsystemserver.domain.payment.entity;

// 외부 PortOne 결제와 포인트 전액 결제를 구분하는 제공자 타입
public enum PaymentProvider {
    // 포트원 통해 외부 카드 결제를 수행한 경우
    PORTONE,
    // 최종 결제금액이 0인 포인트 전액 결제처럼 내부 처리만 수행한 경우
    INTERNAL
}
