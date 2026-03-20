package sparta.paymentsystemserver.domain.membership.dto;

import sparta.paymentsystemserver.domain.membership.entity.MembershipGrade;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;

import java.math.BigDecimal;    // 적립률 저장을 위한 소수타입(Double이나 float 보다 세말함)

public record MembershipResponse(   // 멤버십 정책 목록 조회 응답
        Long id,
        MembershipGradeType membershipCode, // 등급(NORMAL, VIP, VVIP)
        String name,    // 등급 이름
        Long minTotalPaidAmount,    // 해당 등급 최소 누적 결제금액 기준
        Long maxTotalPaidAmount,    // 해당 등급 최대 누적 결제금액 기준 (VVIP는 null)
        BigDecimal earnRate // 포인트 적립률
) {

    // MembershipGrade 엔티티 -> 응답 DTO 반환
    public static MembershipResponse from(MembershipGrade grade) {
        return new MembershipResponse(
                grade.getId(),
                grade.getMembershipCode(),
                grade.getName(),
                grade.getMinTotalPaidAmount(),
                grade.getMaxTotalPaidAmount(),
                grade.getEarnRate()
        );
    }
}
