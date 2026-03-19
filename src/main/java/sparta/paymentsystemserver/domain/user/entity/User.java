package sparta.paymentsystemserver.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGrade;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;
import sparta.paymentsystemserver.global.config.BaseEntity;
import java.util.List;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 이름
    @Column(nullable = false)
    private String name;

    // 로그인 이메일
    @Column(unique = true, nullable = false)
    private String email;

    // 비밀번호
    @Column(nullable = false)
    private String password;

    // PortOne 빌링 고객 식별자
    @Column(nullable = false)
    private String customerUid;

    // 현재 포인트 잔액
    @Column(nullable = false)
    private Long pointBalance = 0L;

    // 현재 멤버십 등급 코드 스냅샷
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipGradeType membershipGrade = MembershipGradeType.NORMAL;

    // 누적 결제 금액 (등급 재계산의 기준이 되는 값)
    @Column(nullable = false)
    private Long totalPaidAmount = 0L;

   // 전화번호
    @Column(nullable = false)
    private String phone;

    public User(String name, String email, String password, String phone, String customerUid) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.customerUid = customerUid;
    }

    // 사용자 정보 수정 - 이름, 전화번호 변경
    public void update(String name, String phone) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
    }


    // 포인트 적립
    public void addPoint(Long amount) {
        if (amount <= 0) {
            // 이 부분은 ErrorCode에 포인트관련해서 에러 추가를 하면 수정
            // 예를 들어 throw new InvalidPointAmountException(); 이런식으로 바꿀 예정
            throw new IllegalArgumentException("적립 포인트는 0보다 커야 합니다.");
        }
        this.pointBalance += amount;
    }


    // 포인트 잔액 감소
    public void subtractPoint(Long amount) {
        if (amount <= 0) {
            // InvalidPointAmountException → ErrorCode.POINT002 → 400 대충 이런식?
            throw new IllegalArgumentException("차감 포인트는 0보다 커야 합니다.");
        }
        if (this.pointBalance < amount) {
            // throw new InsufficientPointException(); 이런식
            throw new IllegalStateException("포인트 잔액이 부족합니다.");
        }
        this.pointBalance -= amount;
    }

    // 사용자의 멤버십 등급 코드 반환
    public MembershipGradeType getMembershipGrade() {
        return this.membershipGrade;
    }

    // 누적 결제 금액 증가
    public void addTotalPaidAmount(Long amount) {
        this.totalPaidAmount += amount;
    }

    // 누적 결제 금액 기준 멤버십 등급 책정
    public void recalculateMembershipGrade(List<MembershipGrade> policies) {
        policies.stream()
                .filter(policy ->
                        this.totalPaidAmount >= policy.getMinTotalPaidAmount()
                                && (policy.getMaxTotalPaidAmount() == null
                                || this.totalPaidAmount <= policy.getMaxTotalPaidAmount()))
                .findFirst()
                .ifPresent(policy -> this.membershipGrade = policy.getMembershipCode());
    }

}