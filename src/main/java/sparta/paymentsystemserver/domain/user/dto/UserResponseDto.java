package sparta.paymentsystemserver.domain.user.dto;

public record UserResponseDto(Long id, String username, String email, String phoneNumber) {
}