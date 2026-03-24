package sparta.paymentsystemserver.domain.payment.service;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 포트원 웹훅 서명 검증을 담당하는 서비스
// 설정값으로 주입된 webhook secret를 사용해서 WebhookVerifier를 초기화한다
// raw body와 포트원 웹훅 헤더를 기준으로 서명을 검증함 검증 실패 시 예외를 발생시켜서 위조되거나 변조된 요청 처리를 막는다
@Slf4j
@Service
public class WebhookSignatureService {

    @Value("${portone.webhook.secret}")
    private String webhookSecret;

    private WebhookVerifier webhookVerifier;

    // 주입된 webhook secret으로 WebhookVerifier를 초기화함
    @PostConstruct
    void init() {
        webhookVerifier = new WebhookVerifier(webhookSecret);
    }

    // 포트원 웹훅의 서명을 검증한다 raw body와 요청 헤더 기준으로 검증하고 검증 실패 시 예외를 발생시킨다
    public void verify(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) {
        try {
            webhookVerifier.verify(
                    rawBody,
                    webhookId,
                    webhookSignature,
                    webhookTimestamp
            );
            log.info("[Webhook] 서명 검증 성공 - webhookId={}", webhookId);
        } catch (WebhookVerificationException exception) {
            log.error("[Webhook] 서명 검증 실패 - webhookId={}", webhookId, exception);
            throw new PaymentException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }
}