package sparta.paymentsystemserver.global.client.dto;

// 결제 서비스에서 사용하는 내부 포트원 결제 정보 dto
public record PortOnePaymentInfo(
        String paymentId,
        String transactionId,
        String status,
        long amount
) {
    // 포트원 조회 결과가 결제 완료 상태인지 판단
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }
}
