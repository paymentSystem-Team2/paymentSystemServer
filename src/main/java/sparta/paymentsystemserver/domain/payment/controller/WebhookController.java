package sparta.paymentsystemserver.domain.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookResponse;
import sparta.paymentsystemserver.domain.payment.service.WebhookService;


import static io.portone.sdk.server.webhook.WebhookVerifier.*;

// 포트원 결제 웹훅의 진입점을 담당하는 컨트롤러
// 1. 포트원으로부터 전달된 raw body와 웹훅 헤더를 수신한다
// 2. Webhook-Id, Webhook-Signature, Webhook-Timestamp 헤더를 추출한다.
// 3. 실제 서명 검증, payload 파싱, 비즈니스 처리는 WebhookService에 위임한다.
@Slf4j
@RestController
@RequestMapping("/api/webhooks/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    // 포트원 웹훅 요청을 수신한다
    @PostMapping
    public PortOneWebhookResponse receiveWebhook(
            @RequestBody String rawBody,
            HttpServletRequest httpServletRequest
    ) {
        log.info(
                "[Webhook][헤더] webhookId={}, webhookTimestamp={}",
                httpServletRequest.getHeader(HEADER_ID),
                httpServletRequest.getHeader(HEADER_TIMESTAMP));

        String webhookId = httpServletRequest.getHeader(HEADER_ID);
        String webhookSignature = httpServletRequest.getHeader(HEADER_SIGNATURE);
        String webhookTimestamp = httpServletRequest.getHeader(HEADER_TIMESTAMP);

        log.info("[Webhook][매핑] webhookId={}", webhookId);

        return webhookService.processWebhook(
                webhookId,
                rawBody,
                webhookSignature,
                webhookTimestamp
        );
    }
}
