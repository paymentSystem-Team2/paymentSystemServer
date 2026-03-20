package sparta.paymentsystemserver.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 구독 플랜 엔티티 사용자가 선택할 요금제 정보를 관리합니다
@Getter
@Entity
@Table(name = "plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부에 노출할 공개 플랜 ID
    @Column(nullable = false, unique = true, length = 40)
    private String planId;

    // 플랜명
    @Column(nullable = false, length = 50)
    private String name;

    // 플랜 금액
    @Column(nullable = false)
    private Long amount;

    // 결제 주기
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingCycle billingCycle;

    // 플랜 설명
    @Column(length = 300)
    private String description;

    // 현재 판매 중인 플랜인지 여부
    @Column(nullable = false)
    private boolean active;

    @Builder
    private Plan(String planId,
                String name,
                Long amount,
                BillingCycle billingCycle,
                String description,
                boolean active
    ) {
        this.planId = planId;
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.description = description;
        this.active = active;
    }
}
