package sparta.paymentsystemserver.domain.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookRequest;
import sparta.paymentsystemserver.domain.payment.dto.WebhookResponse;
import sparta.paymentsystemserver.domain.payment.service.WebhookService;

// 포트원 웹훅 수신 API입니다
// 외부 포트원 서버가 직접 호출하기 때문에 jwt 인증 없이 접근 가능해야 됨
@RestController
@RequestMapping("/api/webhooks/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public WebhookResponse receiveWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestBody PortOneWebhookRequest request
    ) {
        return webhookService.processWebhook(webhookId, request);
    }
}
