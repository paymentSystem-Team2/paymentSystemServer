package sparta.paymentsystemserver.domain.auth.dto;

public record LoginUserResponse(boolean success, String email) {
}
