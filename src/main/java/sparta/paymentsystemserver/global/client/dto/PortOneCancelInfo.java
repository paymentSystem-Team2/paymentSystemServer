package sparta.paymentsystemserver.global.client.dto;

// 환불 서비스에서 사용하는 취소 정보 dto
public record PortOneCancelInfo(
        String paymentId,
        String cancellationId,
        String status,
        long cancelledAmount
) {
    public boolean isCancelled() {
        return "CANCELED".equalsIgnoreCase(status)
                || "SUCCEEDED".equalsIgnoreCase(status);
    }
}
