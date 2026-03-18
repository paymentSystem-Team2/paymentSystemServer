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
public class MembershipGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 등급 이름
    @Column(nullable = false, unique = true)
    private String membershipCode;

    @Column(nullable = false)
    private String name;

    // 등급 시작 구간 (최소 누적 결제금액)
    @Column(nullable = false)
    private Long minTotalPaidAmount;

    // 등급 종료 구간 (최대 누적 결제금액)
    @Column
    private Long maxTotalPaidAmount;

    // 포인트 적립률 (0.01, 0.05, 0.10 예정)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal earnRate;
}
