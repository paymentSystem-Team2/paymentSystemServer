package sparta.paymentsystemserver.global.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.global.client.dto.PortOneBillingKeyPaymentInfo;
import sparta.paymentsystemserver.global.client.dto.PortOneBillingKeyPaymentRequest;
import sparta.paymentsystemserver.global.client.dto.PortOneBillingKeyPaymentResponse;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelRequest;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelResponse;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentResponse;
import sparta.paymentsystemserver.global.config.properties.PortOneProperties;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 포트원 REST API와 통신하는 결제 클라이언트 구현체
// 결제 단건 조회, 결제 취소, 빌링키 기반 단건 청구 요청 담당
@Slf4j
@Component
@RequiredArgsConstructor
public class PortOnePaymentClientImpl implements PortOnePaymentClient {

    private final RestClient.Builder restClientBuilder;
    private final PortOneProperties portOneProperties;

    // 포트원 결제 단건 조회 api를 호출해서 외부 결제 상태를 조회함
    // 결제 검증 단계에서 실제 포트원 결제 정보랑 내부 결제 정보를 비교하기 위해 사용
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

    // 포트원 결제 취소 api를 호출해서 이미 결제된 건을 환불, 취소 처리함
    // 내부 환불 요청 시에 포트원 취소가 실제로 완료되었는지 확인하기 위한 응답 정보로 변환
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

    // 저장된 빌링키로 포트원 빌링키 단건 결제를 요청함
    // 요청 시 빌링키와 함께 storeId, channelKey, 고객 정보, 금액을 전달하고 응답의 pgTxId와 paidAt 값을 기준으로 실제 청구 성공여부 판단
    @Override
    public PortOneBillingKeyPaymentInfo payByBillingKey(
            String paymentId,
            String billingKey,
            String orderName,
            String customerId,
            String fullName,
            String phoneNumber,
            String email,
            Long amount
    ) {
        try {
            String storeId = portOneProperties.getStore().getId();
            String channelKey = portOneProperties.getChannel() == null
                    ? null
                    : portOneProperties.getChannel().get("toss");

            // 포트원 빌링키 결제 스펙에 맞는 요청 본문을 생성함
            PortOneBillingKeyPaymentRequest request = PortOneBillingKeyPaymentRequest.of(
                    billingKey,
                    orderName,
                    customerId,
                    fullName,
                    phoneNumber,
                    email,
                    amount,
                    storeId,
                    channelKey
            );

            log.info("[Billing] 포트원 빌링키 결제 요청 - storeId={}, channelKey={}, paymentId={}, customerId={}, amount={}",
                    storeId,
                    channelKey,
                    paymentId,
                    customerId,
                    amount);

            PortOneBillingKeyPaymentResponse response = buildRestClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}/billing-key")
                            .build(paymentId))
                    .body(request)
                    .retrieve()
                    .body(PortOneBillingKeyPaymentResponse.class);

            if (response == null) {
                throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            // providerPaymentId(pgTxId)와 상태값이 모두 있어야 실제 청구 성공으로 봄
            PortOneBillingKeyPaymentInfo paymentInfo = response.toInfo(paymentId);

            log.info("[Billing] 포트원 빌링키 결제 응답 - paymentId={}, providerPaymentId={}, status={}",
                    paymentInfo.paymentId(),
                    paymentInfo.providerPaymentId(),
                    paymentInfo.status());

            if (paymentInfo.providerPaymentId() == null || paymentInfo.providerPaymentId().isBlank()
                    || paymentInfo.status() == null || paymentInfo.status().isBlank()) {
                throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            return paymentInfo;
        } catch (RestClientResponseException exception) {
            log.error("[Billing] 포트원 빌링키 결제 호출 실패 - statusCode={}, responseBody={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());

            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception exception) {
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // 포트원 api 호출에 공통으로 사용할 RestClient를 생성함
    // baseUrl, 인증 헤더랑 json content-type을 공통 설정으로 적용함
    private RestClient buildRestClient() {
        return restClientBuilder
                .baseUrl(portOneProperties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApi().getSecret())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
