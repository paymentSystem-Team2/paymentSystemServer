package sparta.paymentsystemserver.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.subscription.dto.PlanResponse;
import sparta.paymentsystemserver.domain.subscription.entity.Plan;
import sparta.paymentsystemserver.domain.subscription.repository.PlanRepository;

import java.util.List;

// 플랜 조회 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    // 판매 중인 플랜 목록을 조회
    public List<PlanResponse> getPlans() {
        List<Plan> plans = planRepository.findAllByActiveTrueOrderByAmountAsc();

        return plans.stream()
                .map(PlanResponse::from)
                .toList();
    }

    // 플랜 단건 조회가 필요하면 사용하는 메서드
    public PlanResponse getPlan(String planId) {
        Plan plan = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("플랜을 찾을 수 없습니다."));

        return PlanResponse.from(plan);
    }
}
