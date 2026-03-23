package sparta.paymentsystemserver.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 포트원 웹훅 요청 dto
// 실제 포트원 결제 웹훅은 이런 형태로 들어온다
// - type: Transaction.Ready, Transaction.Paid 같은 이벤트 이름
// - timestamp: PortOne에서 이벤트를 발생시킨 시각
// - data: transactionId, paymentId, storeId가 들어 있는 중첩 객체
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneWebhookRequest(
        String type,
        String timestamp,
        Data data
) {

    // 중첩 payload 안의 paymentId를 꺼냄
    // data가 비어 있는 비정상 요청이면 null을 반환해서 서비스가 유효하지 않은 웹훅으로 처리
    public String paymentId() {
        return data != null ? data.paymentId() : null;
    }

    // 포트원이 보낸 원본 이벤트 이름을 그대로 반환 우리 쪽에서는 providerStatus
    public String providerStatus() {
        return type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String transactionId,
            String paymentId,
            String storeId
    ) {
    }
}
