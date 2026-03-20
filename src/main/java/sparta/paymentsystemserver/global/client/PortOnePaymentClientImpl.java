package sparta.paymentsystemserver.global.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelRequest;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelResponse;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentResponse;
import sparta.paymentsystemserver.global.config.properties.PortOneProperties;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// RestClient 기반 PortOne 연동 구현체
@Component
@RequiredArgsConstructor
public class PortOnePaymentClientImpl implements PortOnePaymentClient {

    private final RestClient.Builder restClientBuilder;
    private final PortOneProperties portOneProperties;

    // paymentId 기준으로 PortOne 결제를 재조회
    @Override
    public PortOnePaymentInfo getPayment(String paymentId) {
        try {
            PortOnePaymentResponse response = buildRestClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("storeId", portOneProperties.getStore().getId())
                            .build(paymentId))
                    .retrieve()
                    .body(PortOnePaymentResponse.class);

            if (response == null) {
                throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            long amount = response.getAmount() != null && response.getAmount().getTotal() != null
                    ? response.getAmount().getTotal()
                    : 0L;

            return new PortOnePaymentInfo(
                    response.getPaymentId() != null ? response.getPaymentId() : paymentId,
                    response.getTxId(),
                    response.getStatus() != null ? response.getStatus() : "UNKNOWN",
                    amount
            );
        } catch (RestClientResponseException exception) {
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // paymentId 기준으로 PortOne 전액 환불을 요청
    @Override
    public PortOneCancelInfo cancelPayment(String paymentId, String reason) {
        try {
            PortOneCancelResponse response = buildRestClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}/cancel")
                            .build(paymentId))
                    .body(new PortOneCancelRequest(
                            portOneProperties.getStore().getId(),
                            reason == null || reason.isBlank() ? "사용자 요청 환불" : reason
                    ))
                    .retrieve()
                    .body(PortOneCancelResponse.class);

            if (response == null) {
                throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
            }

            return new PortOneCancelInfo(
                    response.getPaymentId() != null ? response.getPaymentId() : paymentId,
                    response.getCancelId(),
                    response.getStatus() != null ? response.getStatus() : "UNKNOWN",
                    response.getCanceledAmount() != null ? response.getCanceledAmount() : 0L
            );
        } catch (RestClientResponseException exception) {
            throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
        }
    }

    // PortOne API 호출에 공통으로 사용할 RestClient를 생성
    private RestClient buildRestClient() {
        return restClientBuilder
                .baseUrl(portOneProperties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApi().getSecret())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
