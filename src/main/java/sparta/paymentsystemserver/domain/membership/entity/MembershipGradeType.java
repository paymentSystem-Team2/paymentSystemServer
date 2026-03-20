package sparta.paymentsystemserver.domain.membership.entity;

public enum MembershipGradeType {
    NORMAL, // 일반 누적 결제 5만원 이하, 1% 적립
    VIP,    // 누적 결제 10만원 이하, 5%적립
    VVIP    // 누적 결제 15만원 이상, 10% 적립
}
