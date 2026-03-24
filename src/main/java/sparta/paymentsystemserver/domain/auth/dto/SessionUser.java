package sparta.paymentsystemserver.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.entity.UserRole;

import java.io.Serializable;

// 세션에 저장되는 사용자 정보 DTO
// Spring Session은 세션 객체를 Redis에 직렬화하여 저장 Serializable을 구현해야 함
@Getter
@AllArgsConstructor
public class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;        // 사용자 ID
    private String email;       // 사용자 이메일
    private String name;        // 사용자 이름
    private UserRole role;      // 사용자 역할

    //  User 엔티티로부터 SessionUser 생성
    public SessionUser(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole();
    }

    // 관리자 여부 확인
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    //  일반 사용자 여부 확인
    public boolean isUser() {
        return role == UserRole.USER;
    }
}