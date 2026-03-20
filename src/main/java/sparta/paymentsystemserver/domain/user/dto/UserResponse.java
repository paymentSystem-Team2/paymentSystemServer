package sparta.paymentsystemserver.domain.user.dto;

// 사용자 정보 조회 응답 DTO (포인트 잔액 포함)
public record UserResponse(
        String customerUid,  // PortOne 빌링 고객 식별자
        String email,        // 로그인 이메일
        String name,         // 사용자 이름
        String phone,        // 전화번호
        Long pointBalance    // 현재 포인트 잔액
) {
}