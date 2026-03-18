package sparta.paymentsystemserver.domain.payment.entity;

// 웹훅 수신 이벤트의 내부 처리 상태
public enum WebhookProcessStatus {

    // 웹훅을 저장만 한 직후 상태
    RECEIVED,
    // 공통 컨펌 처리까지 성공한 상태
    PROCESSED,
    // 처리 중에 오류가 난 상태
    FAILED
}
