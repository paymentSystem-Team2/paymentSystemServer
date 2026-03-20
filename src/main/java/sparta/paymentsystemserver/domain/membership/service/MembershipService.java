package sparta.paymentsystemserver.domain.membership.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.membership.dto.MembershipResponse;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradePolicy;
import sparta.paymentsystemserver.domain.membership.repository.MembershipGradeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 읽기 전용
// 1. JPA가 변경감지를 안해서 조회만 하는데 변경사항 추적을 안해서 좋음
// 2. 조회 메서드에서 실수로 데이터를 수정해도 DB에 반영이 안됨
public class MembershipService {

    private final MembershipGradeRepository membershipGradeRepository;

    // 멤버십 정책 목록 조회
    public List<MembershipResponse> getMembershipPolicies() {
        return membershipGradeRepository.findAllByOrderByMinTotalPaidAmountAsc()
                .stream()
                .map(MembershipResponse::from)
                .toList();
    }

    public List<MembershipGradePolicy> findPolicyByType() {
        return membershipGradeRepository.findAll();
    }

}
