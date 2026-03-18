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

    @Column(nullable = false, unique = true)
    private String membershipCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long minTotalPaidAmount;

    @Column
    private Long maxTotalPaidAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal earnRate;
}
