package sparta.paymentsystemserver.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
}