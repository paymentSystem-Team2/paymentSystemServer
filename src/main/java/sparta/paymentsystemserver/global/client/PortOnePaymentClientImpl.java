package sparta.paymentsystemserver.global.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.global.config.properties.PortOneProperties;
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
            Map<String, Object> body = buildRestClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .build(paymentId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> data = nestedMap(body, "data");
            Map<String, Object> payment = nestedMap(data, "payment");
            if (payment == null) {
                payment = nestedMap(body, "payment");
            }

            Map<String, Object> source = payment != null ? payment : body;

            return new PortOnePaymentInfo(
                    stringValue(source, "paymentId", paymentId),
                    firstNonBlank(
                            stringValue(source, "txId", null),
                            stringValue(source, "transactionId", null),
                            stringValue(body, "txId", null),
                            stringValue(body, "transactionId", null)
                    ),
                    stringValue(source, "status", "UNKNOWN"),
                    firstLong(source, "amount", "totalAmount", "paidAmount")
            );
        } catch (RestClientResponseException exception) {
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // paymentId 기준으로 포트원 전액 환불을 요청 사유 없으면 기본 메시지 넣어서 요청
    @Override
    public PortOneCancelInfo cancelPayment(String paymentId, String reason) {
        try {
            Map<String, Object> body = buildRestClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}/cancel")
                            .build(paymentId))
                    .body(Map.of(
                            "storeId", portOneProperties.getStore().getId(),
                            "reason", reason == null || reason.isBlank() ? "사용자 요청 환불" : reason
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> data = nestedMap(body, "data");
            Map<String, Object> cancellation = nestedMap(data, "cancellation");
            if (cancellation == null) {
                cancellation = nestedMap(body, "cancellation");
            }

            Map<String, Object> source = cancellation != null ? cancellation : body;

            return new PortOneCancelInfo(
                    stringValue(body, "paymentId", paymentId),
                    firstNonBlank(
                            stringValue(source, "cancelId", null),
                            stringValue(source, "cancellationId", null),
                            stringValue(body, "cancelId", null),
                            stringValue(body, "cancellationId", null)
                    ),
                    firstNonBlank(
                            stringValue(source, "status", null),
                            stringValue(body, "status", "UNKNOWN")
                    ),
                    firstLong(source, "canceledAmount", "cancelledAmount", "amount")
            );
        } catch (RestClientResponseException exception) {
            throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
        }
    }

    // PortOne API 호출용 RestClient를 공통 설정과 함께 생성
    private RestClient buildRestClient() {
        return restClientBuilder
                .baseUrl(portOneProperties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApi().getSecret())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // 응답 본문(json)에서 문자열 값을 읽는 보조 메서드
    private String stringValue(Map<String, Object> body, String key, String defaultValue) {
        Object value = body == null ? null : body.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    // 여러 후보 문자열 중에 비어 있지 않은 첫 번째 값을 반환한다
    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    // 후보 key 순서대로 확인하면서 포트원 응답에서 첫 번째로 읽을 수 있는 숫자값을 반환
    // 응답 구조가 환경에 따라서 다를 수 있어서 후보기를 유연하게 검사하기 위해서 사용
    private long firstLong(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Long value = extractLong(body, key);
            if (value != null) {
                return value;
            }
        }
        return 0L;
    }

    // 단일 key 기준으로 숫자값을 읽는다 값이 바로 숫자로 오는 경우랑 중첩 구조 모두 처리
    // 읽을 수 있는 숫자가 없으면 null 반환
    private Long extractLong(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);

        // amount: 1000처럼 숫자가 직접 들어온 경우
        if (value instanceof Number number) {
            return number.longValue();
        }

        // 중첩 객체가 아닌 경우는 처리할 수 없음
        if (!(value instanceof Map<?,?> nestedMap)) {
            return null;
        }

        // 중첩 객체 안의 total 값을 우선적으로 확인
        Object total = nestedMap.get("total");
        if (!(total instanceof Number number)) {
            return null;
        }
        return number.longValue();
    }

    // 응답 body 안의 중첩 객체를 꺼냄
    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}
