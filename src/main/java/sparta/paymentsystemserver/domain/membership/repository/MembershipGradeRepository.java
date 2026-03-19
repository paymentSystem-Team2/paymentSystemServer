package sparta.paymentsystemserver.domain.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGrade;

import java.util.List;

public interface MembershipGradeRepository extends JpaRepository<MembershipGrade, Long> {

    // 멤버십 등급 정책 전체 조회
    List<MembershipGrade> findAllByOrderByMinTotalPaidAmountAsc();
}
