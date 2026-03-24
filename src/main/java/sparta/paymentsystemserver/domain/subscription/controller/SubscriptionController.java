package sparta.paymentsystemserver.domain.subscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.subscription.dto.CreateSubscriptionRequest;
import sparta.paymentsystemserver.domain.subscription.dto.SubscriptionResponse;
import sparta.paymentsystemserver.domain.subscription.dto.UpdateSubscriptionRequest;
import sparta.paymentsystemserver.domain.subscription.service.SubscriptionService;

// 구독 생성/조회/수정 api
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 구독 생성 api 빌링키를 결제수단으로 등록하고 실제 과금은 없이 구독 계약 정보만 생성
    @PostMapping
    public SubscriptionResponse createSubscription(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        return subscriptionService.createSubscription(loginUserData.userId(), request);
    }

    // 내 구독 상세 조회 api
    @GetMapping("/{subscriptionId}")
    public SubscriptionResponse getSubscription(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @PathVariable String subscriptionId
    ) {
        return subscriptionService.getSubscription(loginUserData.userId(), subscriptionId);
    }

    // 구독 상태 변경 api
    @PatchMapping("/{subscriptionId}")
    public SubscriptionResponse updateSubscription(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @PathVariable String subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        return subscriptionService.updateSubscription(loginUserData.userId(), subscriptionId, request);
    }
}
