package sparta.paymentsystemserver.domain.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.subscription.dto.PlanResponse;
import sparta.paymentsystemserver.domain.subscription.service.PlanService;

import java.util.List;

// 구독 플랜 조회 api
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // 플랜 목록 조회 api
    @GetMapping
    public List<PlanResponse> getPlans() {
        return planService.getPlans();
    }

    // 플랜 단건 조회 api
    @GetMapping("/{planId}")
    public PlanResponse getPlan(@PathVariable String planId) {
        return planService.getPlan(planId);
    }
}
