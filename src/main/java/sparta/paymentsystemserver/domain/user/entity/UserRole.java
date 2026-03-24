package sparta.paymentsystemserver.domain.user.entity;

public enum UserRole {  // 사용자 역할 Enum, 일반 사용자와 관리자를 구분
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}