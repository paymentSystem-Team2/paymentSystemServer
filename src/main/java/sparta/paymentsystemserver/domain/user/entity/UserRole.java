package sparta.paymentsystemserver.domain.user.entity;

public enum UserRole {  // 사용자 역할 Enum, 일반 사용자와 관리자를 구분
    USER("일반 사용자"),
    ADMIN("관리자");
    // Enum으로 하는 이유: 오타 방지, DB에도 String으로 저장돼서 효율적임, 역할 추가/변경이 쉬움

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}