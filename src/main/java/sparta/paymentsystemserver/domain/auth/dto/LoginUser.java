package sparta.paymentsystemserver.domain.auth.dto;

public record LoginUser(String token,String refreshToken, boolean success, String email){
}
