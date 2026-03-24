package sparta.paymentsystemserver.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;
import sparta.paymentsystemserver.domain.point.exception.InsufficientPointException;
import sparta.paymentsystemserver.domain.point.exception.InvalidPointException;
import sparta.paymentsystemserver.global.config.BaseEntity;
import sparta.paymentsystemserver.domain.user.entity.UserRole;

import static sparta.paymentsystemserver.global.exception.ErrorCode.POINT_AMOUNT_INVALID;
import static sparta.paymentsystemserver.global.exception.ErrorCode.POINT_BALANCE_INSUFFICIENT;

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
    // OAuth 도입으로 필수 제거
    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column
    private String providerId;

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

    // 사용자 역할
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    public User(String name, String email, String password, String phone, String customerUid) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.customerUid = customerUid;
    }

    // Google OAuth 전용 생성자
    public User(String name, String email, String phone, String customerUid, AuthProvider provider, String providerId) {
        this.name = name;
        this.email = email;
        this.password = null;
        this.phone = phone;
        this.customerUid = customerUid;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 사용자 정보 수정 - 이름, 전화번호 변경
    public void update(String name, String phone) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
    }

    // 포인트 적립
    public void addPoint(Long amount) {
        if (amount <= 0) {
            throw new InvalidPointException(POINT_AMOUNT_INVALID);
        }
        this.pointBalance += amount;
    }

    // 포인트 잔액 감소
    public void subtractPoint(Long amount) {
        if (amount <= 0) {
            throw new InvalidPointException(POINT_AMOUNT_INVALID);
        }
        if (this.pointBalance < amount) {
            throw new InsufficientPointException(POINT_BALANCE_INSUFFICIENT);
        }
        this.pointBalance -= amount;
    }

    public void subtractTotalPaidAmount(Long amount) {
        this.totalPaidAmount = Math.max(this.totalPaidAmount - amount, 0L);
    }

    public void addTotalPaidAmount(Long amount) {
        this.totalPaidAmount += amount;
    }

    public void updateGrade(MembershipGradeType grade) {
        this.membershipGrade = grade;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

}