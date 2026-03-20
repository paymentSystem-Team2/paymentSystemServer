package sparta.paymentsystemserver.domain.membership.dto;

import sparta.paymentsystemserver.domain.membership.entity.MembershipGrade;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.math.BigDecimal;

public record MyMembershipResponse( // 사용자 멤버십 등급 조회 응답 DTO
                                    MembershipGradeType membershipGrade,    // 현재 멤버십 등급
                                    Long totalPaidAmount  // 누적 결제금액
                                    //BigDecimal earnRate // 현재 등급의 포인트 적립률
) {

    // User 엔티티 + 현재 등급 정책 -> 응답 DTO 변환
    public static MyMembershipResponse of(User user) {
        return new MyMembershipResponse(
                user.getMembershipGrade(),
                user.getTotalPaidAmount()
        );
    }
}