package sparta.paymentsystemserver.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 포트원 빌링키 단건 결제 응답 dto
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyPaymentResponse(
        Payment payment
) {

    // 포트원 빌링키 결제 응답의 중첩 payment 객체. 결제 완료 여부와 PG 거래 식별자를 담음
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            String pgTxId,
            String paidAt
    ) {
    }

    // 포트원 빌링키 결제 응답에서 필요한 값만 추려서 내부 결제 정보 객체로 변환
    public PortOneBillingKeyPaymentInfo toInfo(String paymentId) {
        if (payment == null) {
            return new PortOneBillingKeyPaymentInfo(paymentId, null, null);
        }

        String status = payment.paidAt() != null && !payment.paidAt().isBlank()
                ? "PAID"
                : null;

        return new PortOneBillingKeyPaymentInfo(
                paymentId,
                status,
                payment.pgTxId()
        );
    }
}

