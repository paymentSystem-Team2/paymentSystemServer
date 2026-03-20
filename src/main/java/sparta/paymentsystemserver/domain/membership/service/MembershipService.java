package sparta.paymentsystemserver.domain.membership.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.membership.dto.MembershipResponse;
import sparta.paymentsystemserver.domain.membership.dto.MyMembershipResponse;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGrade;
import sparta.paymentsystemserver.domain.membership.exception.MembershipException;
import sparta.paymentsystemserver.domain.membership.repository.MembershipGradeRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.exception.UserNotFoundException;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 읽기 전용
// 1. JPA가 변경감지를 안해서 조회만 하는데 변경사항 추적을 안해서 좋음
// 2. 조회 메서드에서 실수로 데이터를 수정해도 DB에 반영이 안됨
public class MembershipService {

    private final MembershipGradeRepository membershipGradeRepository;
    private final UserRepository userRepository;

    // 멤버십 정책 목록 조회
    public List<MembershipResponse> getMembershipPolicies() {
        return membershipGradeRepository.findAllByOrderByMinTotalPaidAmountAsc()
                .stream()
                .map(MembershipResponse::from)
                .toList();
    }

    // 내 멤버십 등급 조회
    public MyMembershipResponse getMyGrade(Long userId) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // User에서 바로 MembershipGrade 꺼내기(추가 DB 조회X)
        MembershipGrade policy = user.getMembershipGradePolicy();

        if (policy == null) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_GRADE_NOT_FOUND);
        }

        // 사용자 정보 + 해당 등급 정책 → 응답 DTO 조합
        return MyMembershipResponse.of(user, policy);
    }

    // 누적 결제금액 기준 등급 재계산 (결제/환불 완료 후 호출)
    @Transactional
    public void recalculate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        List<MembershipGrade> policies = membershipGradeRepository
                .findAllByOrderByMinTotalPaidAmountAsc();

        user.recalculateMembershipGrade(policies);
    }
}
