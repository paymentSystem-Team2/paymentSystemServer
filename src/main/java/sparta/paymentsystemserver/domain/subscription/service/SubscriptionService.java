package sparta.paymentsystemserver.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.subscription.dto.CreateSubscriptionRequest;
import sparta.paymentsystemserver.domain.subscription.dto.SubscriptionResponse;
import sparta.paymentsystemserver.domain.subscription.dto.UpdateSubscriptionRequest;
import sparta.paymentsystemserver.domain.subscription.entity.PaymentMethod;
import sparta.paymentsystemserver.domain.subscription.entity.Plan;
import sparta.paymentsystemserver.domain.subscription.entity.Subscription;
import sparta.paymentsystemserver.domain.subscription.entity.SubscriptionStatus;
import sparta.paymentsystemserver.domain.subscription.exception.SubscriptionException;
import sparta.paymentsystemserver.domain.subscription.repository.PaymentMethodRepository;
import sparta.paymentsystemserver.domain.subscription.repository.PlanRepository;
import sparta.paymentsystemserver.domain.subscription.repository.SubscriptionRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.util.List;

// 구독 생성/조회/상태 변경을 담당하는 서비스
// 이 서비스는 실제 과금을 담당하지 않고 빌링키를 결제수단으로 저장. 구독 계약 정보만 생성하거나 조회하고 수정하는 역할만 맡는다
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PublicIdGenerator publicIdGenerator;

    // 구독 생성 처리 순서
    // 1. 로그인 사용자 조회 하고 요청 customerUid가 현재 사용자 정보와 일치하는지 검증
    // 2. 활성 플랜을 조회하고 요청 금액과 서버 플랜 금액이 일치하는지 검증
    // 3. 빌링키를 현재 사용자의 활성 결제수단으로 등록하고 중복 여부를 검증한다
    // 4. 실제 과금은 없이 구독 엔티티만 생성한다
    @Transactional
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        User user = getUser(userId);
        validateCustomerUid(user, request.customerUid());

        Plan plan = getActivePlan(request.planId());
        validateRequestedAmount(plan, request.amount());

        PaymentMethod paymentMethod = registerPaymentMethod(user, request);
        validateDuplicatedSubscription(userId);

        String subscriptionId = publicIdGenerator.generate("SUB");
        Subscription subscription = Subscription.create(subscriptionId, user, plan, paymentMethod);
        subscriptionRepository.save(subscription);

        log.info("[Subscription] 구독 생성 완료 - userId={}, subscriptionId={}, planId={}",
                userId,
                subscription.getSubscriptionId(),
                plan.getPlanId());

        return SubscriptionResponse.from(subscription);
    }

    // 내 구독 1건을 조회함
    public SubscriptionResponse getSubscription(Long userId, String subscriptionId) {
        Subscription subscription = getSubscriptionDetail(userId, subscriptionId);
        return SubscriptionResponse.from(subscription);
    }

    // 구독 상태를 변경함
    @Transactional
    public SubscriptionResponse updateSubscription(Long userId, String subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = getSubscriptionDetail(userId, subscriptionId);

        // 구독 해지
        if (request.isCancelAction()) {
            cancelSubscription(subscription);
            return SubscriptionResponse.from(subscription);
        }

        // 다음 청구부터 적용할 플랜 변경을 예약함
        if (request.isChangePlanAction()) {
            changePlan(subscription, request.planId());
            return SubscriptionResponse.from(subscription);
        }

        // 구독 해지 예약을 취소함
        if (request.isResumeAction()) {
            resumeSubscription(subscription);
            return SubscriptionResponse.from(subscription);
        }

        throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ACTION_INVALID);
    }

    // 현재 로그인 사용자 조회
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.USER_NOT_FOUND));
    }

    // 현재 판매 중인 플랜 조회
    private Plan getActivePlan(String planId) {
        Plan plan = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.PLAN_NOT_FOUND));

        if (!plan.isActive()) {
            throw new SubscriptionException(ErrorCode.PLAN_NOT_ACTIVE);
        }

        return plan;
    }

    // 요청 customerUid가 로그인 사용자 소유인지 검증함
    private void validateCustomerUid(User user, String customerUid) {
        if (!user.getCustomerUid().equals(customerUid)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_CUSTOMER_UID_MISMATCH);
        }
    }

    // 프론트가 보낸 금액이랑 서버 플랜 금액이 일치하는지 검증
    private void validateRequestedAmount(Plan plan, Long requestedAmount) {
        if (!plan.getAmount().equals(requestedAmount)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_AMOUNT_MISMATCH);
        }
    }

    // 빌링키를 현재 사용자의 활성 결제수단으로 등록한다
    // - 같은 사용자의 기존 활성 결제수단은 비활성화하고 동일 빌링키가 이미 저장되어 있으면 기존 엔티티 재활성화해서 재사용
    // - 처음 보는 빌링키면 새 엔티티 생성
    private PaymentMethod registerPaymentMethod(User user, CreateSubscriptionRequest request) {
        List<PaymentMethod> activeMethods = paymentMethodRepository.findAllByUserIdAndActiveTrue(user.getId());
        PaymentMethod existing = paymentMethodRepository.findByUserIdAndBillingKey(user.getId(), request.billingKey())
                .orElse(null);

        for (PaymentMethod method : activeMethods) {
            if (existing == null || !method.getId().equals(existing.getId())) {
                method.deactivate();
            }
        }

        if (existing != null) {
            existing.activate();
            return existing;
        }

        PaymentMethod paymentMethod = PaymentMethod.create(
                user,
                request.customerUid(),
                request.billingKey(),
                "PORTONE",
                null
        );

        return paymentMethodRepository.save(paymentMethod);
    }

    // 사용자의 중복 구독 여부 검증
    // PENDING, ACTIVE, SUSPENDED 상태의 구독이 있으면 새 구독을 만들 수 없도록 막음
    private void validateDuplicatedSubscription(Long userId) {
        if (subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PENDING).isPresent()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        if (subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).isPresent()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        if (subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.SUSPENDED).isPresent()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_SUSPENDED_EXISTS);
        }
    }

    // 내 구독 상세를 조회한다
    private Subscription getSubscriptionDetail(Long userId, String subscriptionId) {
        return subscriptionRepository.findDetailBySubscriptionIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    // 구독 해지 요청을 처리함 현재 ACTIVE인 구독은 즉시 종료하지 않고
    // 현재 이용기간이 끝난 뒤에 해지되도록 예약함
    private void cancelSubscription(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXPIRED);
        }

        if (subscription.isCancelAtPeriodEnd()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }

        subscription.requestCancel();

        log.info("[Subscription] 구독 해지 요청 완료 - subscriptionId={}, cancelAtPeriodEnd={}",
                subscription.getSubscriptionId(),
                subscription.isCancelAtPeriodEnd());
    }

    // 플랜 변경 예약: 실제 플랜 변경은 즉시 안 하고 다음 청구 성공 시점에 반영되도록 예약만 함
    private void changePlan(Subscription subscription, String planId) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXPIRED);
        }

        if (planId == null || planId.isBlank()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_PLAN_ID_REQUIRED);
        }

        Plan targetPlan = getActivePlan(planId);

        if (subscription.getPlan().getPlanId().equals(targetPlan.getPlanId())) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_CHANGE_NOT_ALLOWED);
        }

        subscription.reservePlanChange(targetPlan, targetPlan.getAmount());

        log.info("[Subscription] 플랜 변경 예약 완료 - subscriptionId={}, currentPlanId={}, pendingPlanId={}",
                subscription.getSubscriptionId(),
                subscription.getPlan().getPlanId(),
                targetPlan.getPlanId());
    }

    // 구독 해지 예약을 취소함
    private void resumeSubscription(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_EXPIRED);
        }

        if (!subscription.isCancelAtPeriodEnd()) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ACTION_INVALID);
        }

        subscription.resumeSubscription();

        log.info("[Subscription] 구독 해지 예약 취소 완료 - subscriptionId={}",
                subscription.getSubscriptionId());
    }
}
