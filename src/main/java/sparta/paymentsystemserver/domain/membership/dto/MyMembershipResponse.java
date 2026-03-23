package sparta.paymentsystemserver.domain.membership.dto;

import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;
import sparta.paymentsystemserver.domain.user.entity.User;

public record MyMembershipResponse( // 사용자 멤버십 등급 조회 응답 DTO
                                    MembershipGradeType membershipGrade,    // 현재 멤버십 등급
                                    Long totalPaidAmount  // 누적 결제금액
) {
    public static MyMembershipResponse of(User user) {
        return new MyMembershipResponse(
                user.getMembershipGrade(),
                user.getTotalPaidAmount()
        );
    }
}