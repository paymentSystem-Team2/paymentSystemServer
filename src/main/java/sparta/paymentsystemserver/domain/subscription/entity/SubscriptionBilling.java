package sparta.paymentsystemserver.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 구독 청구 이력을 저장하는 엔티티
// 정기 결제는 한 번 성공했다고 끝이 아니고 이후 매달 반복 청구를 하니까 매번의 청구 시도를 별도 이력으로 남긴다
// 같은 구독의 동일 청구 기간에 대해서는 billing 이력이 한 개만 생기게 유니크 제약을 둔다
@Getter
@Entity
@Table(
        name = "subscription_billings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_billing_period",
                        columnNames = {"subscription_id", "period_start", "period_end"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionBilling {

    // 청구 이력 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 공개용 청구 ID
    @Column(nullable = false, unique = true, length = 40)
    private String billingId;

    // 어떤 구독에서 발생한 청구인지 연결한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // 포트원이나 외부 결제 시스템의 결제 ID. 즉시청구 정기청구 성공할 때 저장
    @Column(length = 100)
    private String paymentId;

    // 현재 청구 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingStatus status;

    // 실제 청구 금액
    @Column(nullable = false)
    private Long amount;

    // 이번 청구가 담당하는 구간 시작일
    @Column(nullable = false)
    private LocalDateTime periodStart;

    // 이번 청구가 담당하는 구간 종료일
    @Column(nullable = false)
    private LocalDateTime periodEnd;

    // 실제 청구 시도 시각
    @Column(nullable = false)
    private LocalDateTime attemptDate;

    // 실패 사유. 성공할 때는 null
    @Column(length = 300)
    private String failureMessage;

    @Builder
    private SubscriptionBilling(
            String billingId,
            Subscription subscription,
            String paymentId,
            BillingStatus status,
            Long amount,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            LocalDateTime attemptDate,
            String failureMessage
    ) {
        this.billingId = billingId;
        this.subscription = subscription;
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.attemptDate = attemptDate;
        this.failureMessage = failureMessage;
    }

    // 청구 시도를 시작할 때 만드는 초기 이력인데 이후 결제 성공이랑 실패에 따라서 상태를 갱신
    public static SubscriptionBilling pending(String billingId, Subscription subscription) {
        return SubscriptionBilling.builder()
                .billingId(billingId)
                .subscription(subscription)
                .status(BillingStatus.PENDING)
                .amount(subscription.getAmount())
                .periodStart(subscription.getCurrentPeriodStart())
                .periodEnd(subscription.getCurrentPeriodEnd())
                .attemptDate(LocalDateTime.now())
                .build();
    }

    // 청구 성공 처리
    public void markCompleted(String paymentId) {
        this.status = BillingStatus.COMPLETED;
        this.paymentId = paymentId;
        this.failureMessage = null;
    }

    // 청구 실패 처리
    public void markFailed(String failureMessage) {
        this.status = BillingStatus.FAILED;
        this.failureMessage = failureMessage;
    }

}
