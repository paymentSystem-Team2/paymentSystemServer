package sparta.paymentsystemserver.domain.user.dto;

public record UserResponse(Long id, String name, String email, String phone, String customerUid) {
}