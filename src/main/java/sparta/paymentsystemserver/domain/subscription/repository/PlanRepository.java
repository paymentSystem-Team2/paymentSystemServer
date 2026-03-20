package sparta.paymentsystemserver.domain.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.subscription.entity.Plan;

import java.util.List;
import java.util.Optional;

// 플랜 조회 저장소
public interface PlanRepository extends JpaRepository<Plan, Long> {

    // 공개 플랜id 기준 단건 조회
    Optional<Plan> findByPlanId(String planId);

    // 활성화된 플랜만 금액 오름차순으로 조회
    List<Plan> findAllByActiveTrueOrderByAmountAsc();
}
