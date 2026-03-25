package sparta.paymentsystemserver.domain.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.subscription.dto.BillingHistoryResponse;
import sparta.paymentsystemserver.domain.subscription.dto.BillingResponse;
import sparta.paymentsystemserver.domain.subscription.dto.CreateBillingRequest;
import sparta.paymentsystemserver.domain.subscription.service.BillingService;

@RestController
@RequestMapping("/api/subscriptions/{subscriptionId}/billings")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // 저장된 빌링키로 즉시 청구
    @PostMapping
    public BillingResponse createBilling(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @PathVariable String subscriptionId,
            @RequestBody CreateBillingRequest request
    ) {
        return billingService.createBilling(loginUserData.userId(), subscriptionId, request);
    }

    // 특정 구독의 청구 이력 조회
    @GetMapping
    public BillingHistoryResponse getBillingHistory(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @PathVariable String subscriptionId
    ) {
        return billingService.getBillingHistory(loginUserData.userId(), subscriptionId);
    }
}