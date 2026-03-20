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
            RestClient restClient = restClientBuilder
                    .baseUrl(portOneProperties.getApi().getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApi().getSecret())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // PortOne 결제 단건 조회 서버가 결제 확정 전에 실제 포트원 상태와 금액을 직접 검증 위해서 사용
            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("storeId", portOneProperties.getStore().getId())
                            .build(paymentId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            // 포트원 응답에서 서버 내부 검증에 필요한 값만 확인
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
            // 포트원 응답 자체가 실패하거나 예외 발생시 결제 검증 실패로 보냄
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // paymentId 기준으로 포트원 전액 환불을 요청 사유 없으면 기본 메시지 넣어서 요청
    @Override
    public PortOneRefundInfo cancelPayment(String paymentId, String reason) {
        try {
            Map<String, Object> body = buildRestClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}/cancel")
                            .queryParam("storeId", portOneProperties.getStore().getId())
                            .build(paymentId))
                    .body(Map.of(
                            "reason", reason == null ? "사용자 요청 환불" : reason
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return new PortOneRefundInfo(
                    stringValue(body, "paymentId", paymentId),
                    firstNonBlank(
                            stringValue(body, "refundId", null),
                            stringValue(body, "txId", null),
                            stringValue(body, "transactionId", null)
                    ),
                    stringValue(body, "status", "UNKNOWN"),
                    firstLong(body, "cancelledAmount", "amount")
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

    // 여러 후보 문자열 중 비어있지 않은 첫 번째 값을 반환
    // 포트원 응답 필드명이 다를 수도 있으니까 후보 키를 순서대로 검사할 때 사용함
    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
