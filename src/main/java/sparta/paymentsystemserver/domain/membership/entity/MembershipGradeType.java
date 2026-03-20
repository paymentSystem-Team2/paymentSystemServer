package sparta.paymentsystemserver.domain.membership.entity;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum MembershipGradeType {
    NORMAL(0L, new BigDecimal("0.01")),
    VIP(50_000L, new BigDecimal("0.05")),
    VVIP(100_000L, new BigDecimal("0.10"));

    private final Long minTotalPaidAmount;
    private final BigDecimal earnRate;

    MembershipGradeType(Long minTotalPaidAmount, BigDecimal earnRate) {
        this.minTotalPaidAmount = minTotalPaidAmount;
        this.earnRate = earnRate;
    }

}
