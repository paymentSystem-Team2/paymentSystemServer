package sparta.paymentsystemserver.global.client;

// 포트원에서 일반 결제 조회랑 취소를 감싸는 추상화 인터페이스
// 서비스는 이 인터페이스만 의존하고 실제 HTTP 통신 상세는 구현체에서 담당하도록 분리함
public interface PortOnePaymentClient {

    // paymentId 기준으로 포트뭔 결제 단건 조회
    PortOnePaymentInfo getPayment(String paymentId);

    // paymentId 기준으로 전액 환불 요청
    PortOneRefundInfo cancelPayment(String paymentId, String reason);

    record PortOnePaymentInfo(
            String paymentId,
            String transactionId,
            String status,
            long amount
    ) {
        // 포트원 응답이 결제 완료 상태인지 판단하는 메서드
        public boolean isPaid() {
            return "PAID".equalsIgnoreCase(status) || "SUCCEEDED".equalsIgnoreCase(status);
        }
    }

    // 포트원 환불 응답 중 서버가 필요한 정보 담음
    record PortOneRefundInfo(
            String paymentId,
            String refundId,
            String status,
            long cancelledAmount
    ) {
        // PortOne 응답이 환불 완료 상태인지 판단
        public boolean isCancelled() {
            return "CANCELLED".equalsIgnoreCase(status)
                    || "PARTIAL_CANCELLED".equalsIgnoreCase(status)
                    || "REFUNDED".equalsIgnoreCase(status);
        }
    }
}
