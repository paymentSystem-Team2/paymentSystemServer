package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookRequest;
import sparta.paymentsystemserver.domain.payment.dto.PortOneWebhookResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.entity.PaymentWebhookEvent;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.payment.repository.PaymentWebhookEventRepository;

// 포트원 웹훅 처리 서비스
// 현재 웹훅 처리 정책
// - Transaction.Paid: READY 상태 결제를 자동 확정
// - cancel/refund 계열 이벤트: 외부 취소를 내부 상태에 동기화
// - Transaction.Ready 및 그 외 이벤트: 이력만 저장하고 상태는 바꾸지 않음
// 모든 웹훅은 payment_webhook_events 테이블에 먼저 저장
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final String PAID_EVENT_TYPE = "Transaction.Paid";

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RefundService refundService;

    public PortOneWebhookResponse processWebhook(String webhookId, PortOneWebhookRequest request) {
        String paymentId = request.paymentId();
        String providerStatus = request.providerStatus();
        String resolvedWebhookId = resolveWebhookId(webhookId, paymentId);

        log.info(
                "[Webhook] 처리 시작 - webhookId={}, paymentId={}, providerStatus={}",
                resolvedWebhookId,
                paymentId,
                providerStatus
        );

        PortOneWebhookResponse validationFailure = validateRequest(paymentId, resolvedWebhookId);
        if (validationFailure != null) {
            return validationFailure;
        }

        // webhook-id를 멱등키로 사용. 같은 이벤트가 재전송되어도 한 번만 처리하고 성공 응답을 돌려줌
        if (paymentWebhookEventRepository.existsByWebhookId(resolvedWebhookId)) {
            log.info("[Webhook] 중복 웹훅이라 처리 생략 - webhookId={}", resolvedWebhookId);
            return success("이미 처리된 웹훅입니다.");
        }

        PaymentWebhookEvent event = createAndSaveEvent(resolvedWebhookId, paymentId, providerStatus, request);
        Payment payment = paymentRepository.findByPaymentId(paymentId).orElse(null);

        if (payment == null) {
            event.markFailed("결제 정보를 찾을 수 없습니다.");
            log.warn("[Webhook] 결제 정보를 찾지 못했습니다 - paymentId={}", paymentId);
            return failure("결제 정보를 찾을 수 없습니다.");
        }

        if (isCancellationEvent(providerStatus)) {
            return handleCancellationEvent(payment, request, event);
        }

        if (isPaidEvent(providerStatus)) {
            return handlePaidEvent(payment, paymentId, resolvedWebhookId, event);
        }

        return handleNonConfirmingEvent(payment, providerStatus, paymentId, event);
    }

    // 임시 식별자
    private String resolveWebhookId(String webhookId, String paymentId) {
        return webhookId != null ? webhookId : "missing-" + paymentId;
    }

    // 필수 값인 paymentId가 빠진 웹훅은 더 진행하지 않음
    private PortOneWebhookResponse validateRequest(String paymentId, String resolvedWebhookId) {
        if (paymentId == null || paymentId.isBlank()) {
            log.error("[Webhook] paymentId가 비어 있습니다 - webhookId={}", resolvedWebhookId);
            return failure("paymentId가 없어 웹훅을 처리할 수 없습니다.");
        }
        return null;
    }

    // 실제 비즈니스 처리 전에 웹훅 원본을 먼저 저장 이후 컨펌 환불 동기화가 실패해도 어떤 이벤트가 왔는지는 남김
    private PaymentWebhookEvent createAndSaveEvent(
            String webhookId,
            String paymentId,
            String providerStatus,
            PortOneWebhookRequest request
    ) {
        String rawPayload = buildRawPayload(providerStatus, paymentId, request);

        PaymentWebhookEvent event = PaymentWebhookEvent.received(
                webhookId,
                paymentId,
                providerStatus,
                rawPayload
        );

        log.info("[Webhook] 이벤트 저장 시작 - webhookId={}, rawPayload={}", webhookId, rawPayload);
        paymentWebhookEventRepository.saveAndFlush(event);
        log.info("[Webhook] 이벤트 저장 완료 - webhookId={}, eventId={}", webhookId, event.getId());

        return event;
    }

    private String buildRawPayload(String providerStatus, String paymentId, PortOneWebhookRequest request) {
        return "type=" + providerStatus
                + ", paymentId=" + paymentId
                + ", transactionId=" + (request.data() != null ? request.data().transactionId() : null)
                + ", storeId=" + (request.data() != null ? request.data().storeId() : null);
    }

    // 외부에서 취소/환불이 일어난 경우에는 포트원 취소 API를 다시 호출하지 않음
    // 이미 취소된 결과를 기준으로 우리 내부 결제/주문/포인트 상태만 맞춘다
    private PortOneWebhookResponse handleCancellationEvent(
            Payment payment,
            PortOneWebhookRequest request,
            PaymentWebhookEvent event
    ) {
        refundService.syncRefundFromWebhook(
                payment.getPaymentId(),
                request.data() != null ? request.data().transactionId() : null,
                "PortOne 웹훅 취소/환불 동기화"
        );

        markEventProcessed(event);
        log.info(
                "[Webhook] 취소/환불 이벤트 동기화 완료 - providerStatus={}, paymentId={}",
                request.providerStatus(),
                payment.getPaymentId()
        );
        return success("취소/환불 웹훅을 동기화했습니다.");
    }

    // Paid 이벤트만 실제 결제 확정 대상으로 처리
    private PortOneWebhookResponse handlePaidEvent(
            Payment payment,
            String paymentId,
            String resolvedWebhookId,
            PaymentWebhookEvent event
    ) {
        // 사용자가 이미 수동 확정을 눌렀다면 웹훅은 멱등하게 성공 처리
        if (payment.getStatus() == PaymentStatus.PAID) {
            markEventProcessed(event);
            log.info("[Webhook] 이미 결제 완료 상태입니다 - webhookId={}, paymentId={}", resolvedWebhookId, paymentId);
            return success("이미 확정된 결제입니다.");
        }

        // READY가 아닌 상태라면 다른 흐름에서 이미 최종 처리된 상태
        if (payment.getStatus() != PaymentStatus.READY) {
            markEventProcessed(event);
            log.info(
                    "[Webhook] 이미 최종 처리된 결제 상태입니다 - status={}, webhookId={}, paymentId={}",
                    payment.getStatus(),
                    resolvedWebhookId,
                    paymentId
            );
            return success("이미 처리된 결제 상태입니다.");
        }

        try {
            log.info("[Webhook] 결제 자동 확정 시작 - paymentId={}", paymentId);
            paymentService.confirmPaymentByWebhook(paymentId);
            log.info("[Webhook] 결제 자동 확정 성공 - paymentId={}", paymentId);
            markEventProcessed(event);
            log.info("[Webhook] 이벤트 처리 완료 - webhookId={}, eventId={}", resolvedWebhookId, event.getId());
            return success("웹훅 처리가 완료되었습니다.");
        } catch (Exception e) {
            log.error("[Webhook] 웹훅 처리 실패 - webhookId={}, paymentId={}", resolvedWebhookId, paymentId, e);
            markEventFailed(event, e.getMessage());
            log.info(
                    "[Webhook] 실패 이력 저장 완료 - webhookId={}, eventId={}, reason={}",
                    resolvedWebhookId,
                    event.getId(),
                    e.getMessage()
            );
            return failure("웹훅 처리가 실패했습니다.");
        }
    }

    // 확정 대상이 아닌 이벤트는 이력만 남기고 종료
    private PortOneWebhookResponse handleNonConfirmingEvent(
            Payment payment,
            String providerStatus,
            String paymentId,
            PaymentWebhookEvent event
    ) {
        markEventProcessed(event);
        log.info(
                "[Webhook] 확정 대상이 아닌 이벤트로 기록만 남겼습니다 - providerStatus={}, paymentStatus={}, paymentId={}",
                providerStatus,
                payment.getStatus(),
                paymentId
        );
        return success("확정 대상이 아닌 웹훅 이벤트입니다.");
    }

    private boolean isPaidEvent(String providerStatus) {
        return PAID_EVENT_TYPE.equals(providerStatus);
    }

    private boolean isCancellationEvent(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return false;
        }

        String normalized = providerStatus.toLowerCase();
        return normalized.contains("cancel") || normalized.contains("refund");
    }

    private PortOneWebhookResponse success(String message) {
        return new PortOneWebhookResponse(true, message);
    }

    private PortOneWebhookResponse failure(String message) {
        return new PortOneWebhookResponse(false, message);
    }

    // 상태 변경은 바로 저장
    private void markEventProcessed(PaymentWebhookEvent event) {
        event.markProcessed();
        paymentWebhookEventRepository.saveAndFlush(event);
    }

    // 결제 확정 중 예외가 나더라도 실패 이력은 남아야 하니까 실패 상태도 바로 저장
    private void markEventFailed(PaymentWebhookEvent event, String failureReason) {
        event.markFailed(failureReason);
        paymentWebhookEventRepository.saveAndFlush(event);
    }
}
