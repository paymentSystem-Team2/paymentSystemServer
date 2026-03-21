package sparta.paymentsystemserver.global.client;

import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;

// 포트원 결제 조회/취소
public interface PortOnePaymentClient {

    // paymentId 기준으로 포트원 결제 단건을 조회
    PortOnePaymentInfo getPayment(String paymentId);

    // paymentId 기준으로 포트원 전액 환불 요청
    PortOneCancelInfo cancelPayment(String paymentId, String reason);
}
