package sparta.paymentsystemserver.global.client.dto;

// 포트원 빌링키 단건 결제 요청 dto
public record PortOneBillingKeyPaymentRequest(
        String billingKey,
        String orderName,
        Customer customer,
        Amount amount,
        String storeId,
        String channelKey,
        String currency
) {
    // 내부 결제 정보로부터 포트원 빌링키 결제 요청 dto를 생성
    public static PortOneBillingKeyPaymentRequest of(
            String billingKey,
            String orderName,
            String customerId,
            String fullName,
            String phoneNumber,
            String email,
            Long amount,
            String storeId,
            String channelKey
    ) {
        return new PortOneBillingKeyPaymentRequest(
                billingKey,
                orderName,
                new Customer(customerId, fullName, phoneNumber, email),
                new Amount(amount),
                storeId,
                channelKey,
                "KRW"
        );
    }

    // 포트원 결제 요청에 포함되는 고객 정보 객체
    public record Customer(
            String id,
            String fullName,
            String phoneNumber,
            String email
    ) {
    }

    // 포트원 결제 요청에 포함되는 결제 금액 객체
    public record Amount(
            Long total
    ) {
    }
}