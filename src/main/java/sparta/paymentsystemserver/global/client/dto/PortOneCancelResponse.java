package sparta.paymentsystemserver.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 포트원 결제 취소 응답 dto
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneCancelResponse(
        @JsonProperty("id")
        String paymentId,
        Cancellation cancellation
) {
    // 외부에서 사용되는 dto를 내부에 취소 정보 dto로 변환
    public PortOneCancelInfo toCancelInfo(String fallbackPaymentId) {
        if (cancellation == null) {
            return new PortOneCancelInfo(
                    paymentId != null ? paymentId : fallbackPaymentId,
                    null,
                    "UNKNOWN",
                    0L
            );
        }

        return new PortOneCancelInfo(
                paymentId != null ? paymentId : fallbackPaymentId,
                cancellation.id(),
                cancellation.status() != null ? cancellation.status() : "UNKNOWN",
                cancellation.totalAmount() != null ? cancellation.totalAmount() : 0L
        );
    }

    // 포트원 취소 결과 본문
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cancellation(
            String status,
            String id,
            String pgCancellationId,
            Long totalAmount
    ) {
    }
}
