package sparta.paymentsystemserver.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.time.LocalDateTime;

// 사용자의 빌링키를 저장하는 엔티티
// 구독은 매번 사용자의 카드 정보 같은 것들을 다시 받지 않고 포트원에서 발급받은 빌링키를 사용해서 재결제를 수행한다
// 어떤 사용자가 어떤 빌링키를 가지고 있는가 저장하는 역할
@Getter
@Entity
@Table(name = "payment_methods")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod {

    // 결제 수단 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자의 결제수단인지 연결함. 한 사용자는 여러 결제수단을 가질 수 있으니까 ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 포트원에서 고객 식별용으로 사용하는 customerUid. 빌링키 발급하고 조회할 때 함께 사용될 수 있어서 저장
    @Column(nullable = false, length = 100)
    private String customerUid;

    // 포트원에서 발급한 빌링키. 실제 정기결제랑 즉시청구 요청에 사용하는 값
    @Column(nullable = false, length = 100)
    private String billingKey;

    // 결제 제공자를 구분하는데 지금은 포트원만 있긴 함
    @Column(nullable = false, length = 50)
    private String provider;

    // 화면 표시용 카드사 이름 필수 값은 아님
    @Column(length = 50)
    private String cardCompany;

    // 현재 사용 가능한 기본 결제수단인지 여부. 사용자가 카드 변경을 하면 기존 결제수단을 비활성화 한다
    @Column(nullable = false)
    private boolean active;

    // 결제수단 등록 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PaymentMethod(
            User user,
            String customerUid,
            String billingKey,
            String provider,
            String cardCompany,
            boolean active,
            LocalDateTime createdAt
    ) {
        this.user = user;
        this.customerUid = customerUid;
        this.billingKey = billingKey;
        this.provider = provider;
        this.cardCompany = cardCompany;
        this.active = active;
        this.createdAt = createdAt;
    }

    // 새 빌링키를 발급 받아서 저장할 때 사용하는 메서드
    public static PaymentMethod create(
            User user,
            String customerUid,
            String billingKey,
            String provider,
            String cardCompany
    ) {
        return PaymentMethod.builder()
                .user(user)
                .customerUid(customerUid)
                .billingKey(billingKey)
                .provider(provider)
                .cardCompany(cardCompany)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 더 이상 사용하지 않는 결제수단 비활성화. 실제 삭제 대신에 상태값만 변경해서 이력을 남긴다.
    public void deactivate() {
        this.active = false;
    }

    // 기존 결제수단을 다시 활성화. 동일 빌링키를 다시 사용하는 경우에는 새 엔티티를 만들지 않고 기존 엔티티를 재사용하기 위해서
    public void activate() {
        this.active = true;
    }

}
