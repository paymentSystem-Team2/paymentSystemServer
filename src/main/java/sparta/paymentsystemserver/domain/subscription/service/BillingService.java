package sparta.paymentsystemserver.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.subscription.dto.BillingHistoryItemResponse;
import sparta.paymentsystemserver.domain.subscription.dto.BillingHistoryResponse;
import sparta.paymentsystemserver.domain.subscription.dto.BillingResponse;
import sparta.paymentsystemserver.domain.subscription.dto.CreateBillingRequest;
import sparta.paymentsystemserver.domain.subscription.entity.Subscription;
import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionBilling;
import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionStatus;
import sparta.paymentsystemserver.domain.subscription.exception.SubscriptionException;
import sparta.paymentsystemserver.domain.subscription.repository.SubscriptionBillingRepository;
import sparta.paymentsystemserver.domain.subscription.repository.SubscriptionRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOneBillingKeyPaymentInfo;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.util.List;


// 구독 청구 실행과 청구 이력 조회 담당하는 서비스
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PublicIdGenerator publicIdGenerator;

    // 즉시 청구 실행
    @Transactional
    public BillingResponse createBilling(Long userId, String subscriptionId, CreateBillingRequest request) {
        Subscription subscription = getSubscriptionDetail(userId, subscriptionId);
        return processBilling(subscription);
    }

    // 스케줄러가 자동 청구할 때 사용하는 내부 청구 메서드
    @Transactional
    public BillingResponse createScheduledBilling(Subscription subscription) {
        return processBilling(subscription);
    }

    // 실제 청구 실행 공통 로직
    // 1. 현재 구독 상태에서 청구 가능한지 검증
    // 2. 청구 이력을 PENDING 상태로 먼저 저장
    // 3. 포트원 빌링키 결제 수행
    // 4. 성공하면 청구 이력을 COMPLETED로 바꾸고 다음 기간으로 갱신
    // 5. 실패하면 청구 이력을 FAILED로 바꿈 PENDING 상태 그대로 둔다
    private BillingResponse processBilling(Subscription subscription) {
        validateBillingAllowed(subscription);

        if (subscriptionBillingRepository.existsBySubscriptionAndPeriodStartAndPeriodEnd(
                subscription,
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        )) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_BILLING_NOT_ALLOWED);
        }

        String billingId = publicIdGenerator.generate("BIL");
        String paymentId = publicIdGenerator.generate("PAY");

        SubscriptionBilling billing = SubscriptionBilling.pending(billingId, subscription);
        subscriptionBillingRepository.save(billing);

        try {
            PortOneBillingKeyPaymentInfo paymentInfo = portOnePaymentClient.payByBillingKey(
                    paymentId,
                    subscription.getPaymentMethod().getBillingKey(),
                    subscription.getPlan().getName() + " 정기 결제",
                    subscription.getUser().getCustomerUid(),
                    subscription.getUser().getName(),
                    subscription.getUser().getPhone(),
                    subscription.getUser().getEmail(),
                    subscription.getAmount()
            );

            billing.markCompleted(paymentInfo.paymentId());

            if (subscription.getStatus() == SubscriptionStatus.PENDING) {
                subscription.activateFirstPeriod();
            } else if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
                subscription.resumeBillingPeriod();
            } else {
                subscription.renewNextPeriod();
            }

            log.info("[Billing] 청구 성공 - subscriptionId={}, billingId={}, paymentId={}",
                    subscription.getSubscriptionId(),
                    billing.getBillingId(),
                    paymentInfo.paymentId());

            return BillingResponse.from(billing);
        } catch (Exception exception) {
            String failureMessage = exception.getMessage() != null
                    ? exception.getMessage()
                    : "청구 처리 중 알 수 없는 오류가 발생했습니다.";

            billing.markFailed(failureMessage);

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.suspend();
            }

            log.error("[Billing] 청구 실패 - subscriptionId={}, billingId={}, failureMessage={}",
                    subscription.getSubscriptionId(),
                    billing.getBillingId(),
                    failureMessage);

            return BillingResponse.from(billing);
        }
    }

    // 특정 구독 청구 이력 조회
    public BillingHistoryResponse getBillingHistory(Long userId, String subscriptionId) {
        Subscription subscription = getSubscriptionDetail(userId, subscriptionId);

        List<BillingHistoryItemResponse> billings = subscriptionBillingRepository
                .findAllBySubscriptionSubscriptionIdOrderByAttemptDateDesc(subscription.getSubscriptionId())
                .stream()
                .map(BillingHistoryItemResponse::from)
                .toList();

        return new BillingHistoryResponse(billings);
    }

    // 현재 로그인 사용자의 구독 조회
    private Subscription getSubscriptionDetail(Long userId, String subscriptionId) {
        return subscriptionRepository.findDetailBySubscriptionIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    // 현재 상태에서 청구가 가능한지 검증
    // 해지 예약된 구독(cancelAtPeriodEnd=true)는 현재 기간 유지하더라도 다음 청구 수행하면 안 되니까 billing 대상에서 제외
    private void validateBillingAllowed(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED
                || subscription.isCancelAtPeriodEnd()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_BILLING_NOT_ALLOWED);
        }
    }
}

