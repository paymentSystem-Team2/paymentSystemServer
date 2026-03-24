package sparta.paymentsystemserver.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.time.LocalDateTime;

// 실제 사용자의 구독 상태를 저장하는 구독 본체. 누가 어떤 플랜을 어떤 결제 수단으로 어떤 주기 상태로 구독하는지 기록
@Getter
@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    // 내부 디비 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 공개용 구독id
    @Column(nullable = false, unique = false, length = 40)
    private String subscriptionId;

    // 구독한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 현재 적용 중인 플랜
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // 다음 청구부터 적용할 예약 플랜. 변경 요청을 하면 다음 청구 성공 때 이 플랜으로 교체한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_plan_id")
    private Plan pendingPlan;

    // 결제에 사용할 빌링키(결제수단). 구독 생성하는 시점에 어떤 빌링키를 사용할지 연결한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    // 현재 구독 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    // 생성 시점에 기준 실제 청구 금액. 이후에 플랜 가격이 바뀌더라도 당시 금액 이력을 분리해서 보려고 함
    @Column(nullable = false)
    private Long amount;

    // 다음 청구부터 적용할 예약 금액
    private Long pendingAmount;

    // 현재 과금 구간 시작 시간. 예를 들어서 3월 구독 시작일.
    @Column(nullable = false)
    private LocalDateTime currentPeriodStart;

    // 현재 과금 구간 종료 시각. 예를 들어서 3월 구독 종료일.
    @Column(nullable = false)
    private LocalDateTime currentPeriodEnd;

    // 다음 청구 예정 시간. 스케줄러가 이 값을 기준으로 정기 결제를 수행한다
    @Column(nullable = false)
    private LocalDateTime nextBillingAt;

    // 현재 결제 기간 유지하고 다음 결제부터 구독 종료할지 여부
    @Column(nullable = false)
    private boolean cancelAtPeriodEnd;

    // 해지 시각. 해지하지 않았으면 null
    private LocalDateTime cancelledAt;

    // 구독 생성 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;


    @Builder
    private Subscription(
            String subscriptionId,
            User user,
            Plan plan,
            Plan pendingPlan,
            PaymentMethod paymentMethod,
            SubscriptionStatus status,
            Long amount,
            Long pendingAmount,
            LocalDateTime currentPeriodStart,
            LocalDateTime currentPeriodEnd,
            LocalDateTime nextBillingAt,
            boolean cancelAtPeriodEnd,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt
    ) {
        this.subscriptionId = subscriptionId;
        this.user = user;
        this.plan = plan;
        this.pendingPlan = pendingPlan;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.amount = amount;
        this.pendingAmount = pendingAmount;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.nextBillingAt = nextBillingAt;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
    }

    // 구독 생성은 과금이 아니라 구독 계약 정보만 만든다
    public static Subscription create(
            String subscriptionId,
            User user,
            Plan plan,
            PaymentMethod paymentMethod
    ) {
        LocalDateTime now = LocalDateTime.now();

        return Subscription.builder()
                .subscriptionId(subscriptionId)
                .user(user)
                .plan(plan)
                .paymentMethod(paymentMethod)
                .status(SubscriptionStatus.PENDING)
                .amount(plan.getAmount())
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusMonths(1))
                .nextBillingAt(now.plusMonths(1))
                .cancelAtPeriodEnd(false)
                .createdAt(now)
                .build();
    }

    // 구독 해지 요청. ACTIVE 상태에서는 즉시 종료하지 않고 현재 결제 기간이 끝난 뒤에 해지되도록 예약
    public void requestCancel() {
        if (this.status == SubscriptionStatus.ACTIVE) {
            this.cancelAtPeriodEnd = true;
            return;
        }

        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    // 예약 해지된 구독은 최종 종료 상태로 전환한다.
    // 현재 결제 기간이 종료된 뒤에 스케줄러가 호출하는 메서드
    public void completeCancellation() {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelAtPeriodEnd = false;
        this.cancelledAt = LocalDateTime.now();
    }

    // 미납
    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
    }

    // 기간 만료
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    // 다음 청구부터 적용할 플랜/금액 변경 예약
    public void reservePlanChange(Plan pendingPlan, Long pendingAmount) {
        this.pendingPlan = pendingPlan;
        this.pendingAmount = pendingAmount;
    }

    // 예약된 플랜/금액 변경을 실제 반영
    public void applyPendingPlanChange() {
        if (this.pendingPlan != null) {
            this.plan = this.pendingPlan;
            this.amount = this.pendingAmount;
            this.pendingPlan = null;
            this.pendingAmount = null;
        }
    }

    // 첫 결제 성공 시에 첫 구독 주기를 시작하고 ACTIVE로 전환
    public void activateFirstPeriod() {
        LocalDateTime now = LocalDateTime.now();
        applyPendingPlanChange();
        this.currentPeriodStart = now;
        this.currentPeriodEnd = now.plusMonths(1);
        this.nextBillingAt = this.currentPeriodEnd;
        this.status = SubscriptionStatus.ACTIVE;
    }

    // SUSPENDED 상태 재청구 성공 시에 현재 시점을 기준으로 구독 주기를 다시 시작한다
    public void resumeBillingPeriod() {
        LocalDateTime now = LocalDateTime.now();
        applyPendingPlanChange();
        this.currentPeriodStart = now;
        this.currentPeriodEnd = now.plusMonths(1);
        this.nextBillingAt = this.currentPeriodEnd;
        this.status = SubscriptionStatus.ACTIVE;
    }

    // 한 번의 청구가 성공한 뒤에는 다음 이용 구간으로 갱신. 이때 예약된 플랜 변경이 있으면 함께 반영한다
    public void renewNextPeriod() {
        applyPendingPlanChange();
        this.currentPeriodStart = this.currentPeriodEnd;
        this.currentPeriodEnd = this.currentPeriodEnd.plusMonths(1);
        this.nextBillingAt = this.currentPeriodEnd;
        this.status = SubscriptionStatus.ACTIVE;
    }

    // 플랜 가격 정책 변경이 되면 현재 구독 금액을 다음 청구 기준으로 갱신
    public void updateAmount(Long amount) {
        this.amount = amount;
    }

    // 해지 예약을 취소하고 구독 계속 유지
    public void resumeSubscription() {
        this.cancelAtPeriodEnd = false;
    }

}
