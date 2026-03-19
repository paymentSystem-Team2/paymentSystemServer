package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookRequest;
import sparta.paymentsystemserver.domain.payment.dto.WebhookResponse;
import sparta.paymentsystemserver.domain.payment.entity.PaymentWebhookEvent;
import sparta.paymentsystemserver.domain.payment.repository.PaymentWebhookEventRepository;

// 포트원 웹훅 처리 서비스
// webhook-id 기준 멱등 처리과 페이로드가 아니라 paymentId 기반 포트원 재조회 결과를 신뢰
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final PaymentService paymentService;

    @Transactional
    public WebhookResponse processWebhook(String webhookId, PortOneWebhookRequest request) {

        // 이미 같은 webhookId를 처리한 적이 있으면 중복 웹훅으로 간주
        if (paymentWebhookEventRepository.existsByWebhookId(webhookId)) {
            return new WebhookResponse(true, "이미 처리된 웹훅입니다.");
        }

        // 원본 payload를 문자열로 간단하게 저장
        String rawPayload = "paymentId=" + request.paymentId() + ", status=" + request.status();

        PaymentWebhookEvent event = PaymentWebhookEvent.received(
                webhookId,
                request.paymentId(),
                request.status(),
                rawPayload
        );

        paymentWebhookEventRepository.save(event);

        try {
            // 웹훅 payload를 그대로 믿지 않고 paymentId 기준 재조회 후 공통 confirm 로직 수행
            paymentService.confirmPaymentByWebhook(request.paymentId());

            event.markProcessed();
            return new WebhookResponse(true, "웹훅 처리가 완료되었습니다.");
        } catch (Exception e) {
            event.markFailed(e.getMessage());
            return new WebhookResponse(false, "웹훅 처리에 실패했습니다.");
        }
    }
}
