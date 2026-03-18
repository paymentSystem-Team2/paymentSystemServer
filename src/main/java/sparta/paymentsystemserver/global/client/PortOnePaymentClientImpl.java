package sparta.paymentsystemserver.global.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.global.config.PortOneProperties;
import sparta.paymentsystemserver.global.exception.ErrorCode;

import java.util.Map;

// RestClient 기반 포트원 일반 결제 조회 구현체
// 포트원 공식 문서 결제 단건 조회 API 호출, 서버가 최종 결제 상태를 직접 검증할 수 있도록 함
@Component
@RequiredArgsConstructor
public class PortOnePaymentClientImpl implements PortOnePaymentClient {

    private final RestClient.Builder restClientBuilder;
    private final PortOneProperties portOneProperties;

    // paymentId 기준으로 포트원 결제 다시 조회
    // 중복 처리와 위조 방지를 위함. 최종 기준을 포트원 서버 응답으로 맞춤
    @Override
    public PortOnePaymentInfo getPayment(String paymentId) {
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(portOneProperties.getApi().getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApi().getSecret())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // PortOne 결제 단건 조회
            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("storeId", portOneProperties.getStore().getId())
                            .build(paymentId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return new PortOnePaymentInfo(
                    stringValue(body, "paymentId", paymentId),
                    firstNonBlank(
                            stringValue(body, "txId", null),
                            stringValue(body, "transactionId", null)
                    ),
                    stringValue(body, "status", "UNKNOWN"),
                    firstLong(body, "amount", "totalAmount", "paidAmount")
            );
        } catch (RestClientResponseException e) {
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // 응답 본문(json)에서 문자열 값을 읽는 보조 메서드
    private String stringValue(Map<String, Object> body, String key, String defaultValue) {
        Object value = body == null ? null : body.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }


    // amount.total 같은 중첩 응답까지 포함해서 가장 먼저 찾은 수자 값을 반환
    private long firstLong(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body == null ? null : body.get(key);

            if (value instanceof Number number) {
                return number.longValue();
            }

            if (value instanceof Map<?, ?> nestedMap) {
                Object total = nestedMap.get("total");
                if (total instanceof Number number) {
                    return number.longValue();
                }
            }
        }
        return 0L;
    }

    // 여러 후보 문자열 중 비어있지 않은 첫 번째 값을 반환
    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
