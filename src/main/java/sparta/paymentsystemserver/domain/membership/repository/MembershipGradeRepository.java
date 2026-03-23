package sparta.paymentsystemserver.domain.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradePolicy;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;

import java.util.List;
import java.util.Optional;

public interface MembershipGradeRepository extends JpaRepository<MembershipGradePolicy, Long> {

    // 멤버십 등급 정책 전체 조회
    List<MembershipGradePolicy> findAllByOrderByMinTotalPaidAmountAsc();
}
