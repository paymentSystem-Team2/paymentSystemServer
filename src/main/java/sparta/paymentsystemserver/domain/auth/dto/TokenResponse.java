package sparta.paymentsystemserver.domain.auth.dto;

public record TokenResponse (String newAccessToken , String newRefreshToken) {
}
