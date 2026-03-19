package sparta.paymentsystemserver.domain.user.dto;

public record UserResponse(String customerUid,String email, String name, String phone,Long balance) {
}