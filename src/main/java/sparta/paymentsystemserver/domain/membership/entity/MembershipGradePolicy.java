package sparta.paymentsystemserver.domain.membership.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "membership_policies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipGradePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private MembershipGradeType membershipCode;

    // 등급 시작 구간 (최소 누적 결제금액)
    @Column(nullable = false)
    private Long minTotalPaidAmount;

    // 포인트 적립률 (0.01, 0.05, 0.10 예정)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal earnRate;
}
