package sparta.paymentsystemserver.global.client.dto;

// 내부 포트원 결제 정보 dto
public record PortOnePaymentInfo(
        String paymentId,
        String transactionId,
        String status,
        long amount
) {
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }
}
