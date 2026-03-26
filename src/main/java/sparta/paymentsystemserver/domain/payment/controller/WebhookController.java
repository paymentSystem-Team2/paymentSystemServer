package sparta.paymentsystemserver.domain.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookResponse;
import sparta.paymentsystemserver.domain.payment.service.WebhookService;

import static io.portone.sdk.server.webhook.WebhookVerifier.HEADER_ID;
import static io.portone.sdk.server.webhook.WebhookVerifier.HEADER_SIGNATURE;
import static io.portone.sdk.server.webhook.WebhookVerifier.HEADER_TIMESTAMP;

// PortOne 결제 웹훅 요청의 진입점을 담당하는 컨트롤러다.
// 1. PortOne으로부터 전달받은 raw body와 헤더를 수신한다.
// 2. Webhook-Id, Webhook-Signature, Webhook-Timestamp 헤더를 추출한다.
// 3. 실제 서명 검증, payload 파싱, 비즈니스 처리는 WebhookService에 위임한다.
@Slf4j
@RestController
@RequestMapping("/api/webhooks/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    // PortOne 웹훅 요청을 수신한다.
    @PostMapping
    public PortOneWebhookResponse receiveWebhook(
            @RequestBody String rawBody,
            HttpServletRequest httpServletRequest
    ) {
        String webhookId = httpServletRequest.getHeader(HEADER_ID);
        String webhookSignature = httpServletRequest.getHeader(HEADER_SIGNATURE);
        String webhookTimestamp = httpServletRequest.getHeader(HEADER_TIMESTAMP);

        return webhookService.processWebhook(
                webhookId,
                rawBody,
                webhookSignature,
                webhookTimestamp
        );
    }
}
