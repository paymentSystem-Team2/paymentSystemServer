package sparta.paymentsystemserver.domain.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookRequest;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookResponse;
import sparta.paymentsystemserver.domain.payment.service.WebhookService;

import java.util.Collections;

// 포트원 웹훅 진입점 컨트롤러는
// 1. 들어온 헤더와 핵심 payload 값을 로그로 남김
// 2. 실제 비즈니스 처리는 WebhookService에 위임
@Slf4j
@RestController
@RequestMapping("/api/webhooks/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public PortOneWebhookResponse receiveWebhook(
            @RequestHeader(value = "Webhook-Id", required = false) String webhookId,
            @RequestBody PortOneWebhookRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Collections.list(httpServletRequest.getHeaderNames())
                .forEach(name -> log.info("[Webhook][헤더] {}={}", name, httpServletRequest.getHeader(name)));

        // 포트원은 Webhook-Id 헤더를 이벤트의 고유 식별자로 사용
        String resolvedWebhookId = webhookId != null
                ? webhookId
                : httpServletRequest.getHeader("webhook-id");

        log.info("[Webhook][매핑] webhookId={}", resolvedWebhookId);
        log.info(
                "[Webhook][본문] type={}, paymentId={}, transactionId={}, storeId={}",
                request.providerStatus(),
                request.paymentId(),
                request.data() != null ? request.data().transactionId() : null,
                request.data() != null ? request.data().storeId() : null
        );

        return webhookService.processWebhook(resolvedWebhookId, request);
    }
}
