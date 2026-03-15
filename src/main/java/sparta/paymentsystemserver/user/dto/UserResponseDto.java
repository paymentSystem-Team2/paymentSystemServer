package sparta.paymentsystemserver.user.dto;

public record UserResponseDto(Long id, String username, String email, String phoneNumber) {
}