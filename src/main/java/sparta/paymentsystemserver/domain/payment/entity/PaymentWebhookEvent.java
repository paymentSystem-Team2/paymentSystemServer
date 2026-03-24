package sparta.paymentsystemserver.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.global.config.BaseEntity;

// 포트원 웹훅 수신 이력 남기는 엔티티
// 웹훅은 같은 이벤트가 중복 도착할 수 있기 때문에 웹훅아이디를 유니크로 두고 수신 여부랑 처리 결과를 분리해서 기록
@Getter
@Entity
@Table(name = "payment_webhook_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEvent extends BaseEntity {

    // 디비 내부 기본키
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 포트원이 보낸 웹훅 고유 Id 중복 수신 방지 키
    @Column(nullable = false, unique = true, length = 100)
    private String webhookId;

    // 이 이벤트가 가리키는 내부 페이먼트아이디
    @Column(nullable = false, length = 80)
    private String paymentId;

    // 포트원 쪽에 상태 문자열 원본
    @Column(nullable = false, length = 100)
    private String providerStatus;

    // 우리 서버가 이 웹훅을 어떻게 처리했는지 나타내는 내부 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookProcessStatus status;

    // 나중에 추적할 수 있도록 저장하는 원본 페이로드 제이슨
    @Lob
    private String rawPayload;

    // 처리 실패 시에 원인 남기는 필드
    @Column(length = 300)
    private String failureReason;

    @Builder
    private PaymentWebhookEvent(
            String webhookId,
            String paymentId,
            String providerStatus,
            WebhookProcessStatus status,
            String rawPayload
    ) {
        this.webhookId = webhookId;
        this.paymentId = paymentId;
        this.providerStatus = providerStatus;
        this.status = status;
        this.rawPayload = rawPayload;
    }

    // 수신 직후 상태는 RECEIVED로 시작
    public static PaymentWebhookEvent received(String webhookId, String paymentId, String providerStatus, String rawPayload) {
        return PaymentWebhookEvent.builder()
                .webhookId(webhookId)
                .paymentId(paymentId)
                .providerStatus(providerStatus)
                .status(WebhookProcessStatus.RECEIVED)
                .rawPayload(rawPayload)
                .build();
    }

    // 공통 confirm 로직까지 성공적으로 끝났을 때 호출
    public void markProcessed() {
        this.status = WebhookProcessStatus.PROCESSED;
        this.failureReason = null;
    }

    // 처리에 실패했을 때 실패 사유를 남김
    public void markFailed(String failureReason) {
        this.status = WebhookProcessStatus.FAILED;
        this.failureReason = failureReason;
    }

}
