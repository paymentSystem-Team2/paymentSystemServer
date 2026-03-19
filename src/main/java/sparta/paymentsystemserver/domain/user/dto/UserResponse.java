package sparta.paymentsystemserver.domain.user.dto;

public record UserResponse(Long userId, String name, String email, String phone) {
}