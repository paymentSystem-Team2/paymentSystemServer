package sparta.paymentsystemserver.global.client.dto;

// 빌링키 결제 결과를 서비스에서 다루기 위한 내부 dto
public record PortOneBillingKeyPaymentInfo(
        String paymentId,
        String status,
        String providerPaymentId
) {
}