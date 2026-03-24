package sparta.paymentsystemserver.global.client.dto;

// 환불에서 사용하는 내부 포트원 취소 dto
public record PortOneCancelInfo(
        String paymentId,
        String cancellationId,
        String status,
        long cancelledAmount
) {
    // 포트원 취소 응답이 성공 또는 접수 완료 상태인지 판단
    public boolean isCancelled() {
        return "CANCELED".equalsIgnoreCase(status)
                || "SUCCEEDED".equalsIgnoreCase(status)
                || "REQUESTED".equalsIgnoreCase(status);
    }
}
