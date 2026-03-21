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

@Component
@RequiredArgsConstructor
public class PortOnePaymentClientImpl implements PortOnePaymentClient {

    private final RestClient.Builder restClientBuilder;
    private final PortOneProperties portOneProperties;

    // paymentId 기준으로 포트원 결제 재조회
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

            return response.toPaymentInfo(paymentId);
        } catch (RestClientResponseException exception) {
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // paymentId 기준으로 포트원 전액 환불 요청
    @Override
    public PortOneCancelInfo cancelPayment(String paymentId, String reason) {
        try {
            PortOneCancelResponse response = buildRestClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}/cancel")
                            .build(paymentId))
                    .body(PortOneCancelRequest.of(portOneProperties.getStore().getId(), reason))
                    .retrieve()
                    .body(PortOneCancelResponse.class);

            if (response == null) {
                throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
            }

            return response.toCancelInfo(paymentId);
        } catch (RestClientResponseException exception) {
            throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
        }
    }

    // 포트원 호출에 공통으로 사용할 RestClient 생성
    private RestClient buildRestClient() {
        return restClientBuilder
                .baseUrl(portOneProperties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApi().getSecret())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
