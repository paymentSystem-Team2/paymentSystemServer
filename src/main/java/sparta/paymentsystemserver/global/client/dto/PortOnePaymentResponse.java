package sparta.paymentsystemserver.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 포트원 결제 조회 응답 dto 서버 검증에 필요한 결제아이디, 상태, 거래id, 승인 금액 매핑
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOnePaymentResponse {

    @JsonProperty("id")
    private String paymentId;

    private String status;
    private String txId;
    private Amount amount;

    // 내부 결제 정보dto 변환
    public PortOnePaymentInfo toPaymentInfo(String fallbackPaymentId) {
        return new PortOnePaymentInfo(
                paymentId != null ? paymentId : fallbackPaymentId,
                txId,
                status != null ? status : "UNKNOWN",
                resolvedAmount()
        );
    }

    // 포트원 응답 객체에서 승인 금액 꺼냄
    private long resolvedAmount() {
        return amount != null && amount.getTotal() != null
                ? amount.getTotal()
                : 0L;
    }

    // 포트원의 결제 금액 정보 객체
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Long total;
    }
}
