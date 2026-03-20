package sparta.paymentsystemserver.global.client;

import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;

// PortOne 결제 조회/취소를 감싸는 추상화 인터페이스
public interface PortOnePaymentClient {

    // paymentId 기준으로 PortOne 결제 단건을 조회
    PortOnePaymentInfo getPayment(String paymentId);

    // paymentId 기준으로 PortOne 전액 취소(환불)를 요청
    PortOneCancelInfo cancelPayment(String paymentId, String reason);
}
