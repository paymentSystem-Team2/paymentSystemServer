package sparta.paymentsystemserver.user.dto;

import lombok.Getter;
import sparta.paymentsystemserver.user.entity.User;

@Getter
public class UserResponseDto {

    private final Long id;
    private final String username;
    private final String email;
    private final String phoneNumber;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
    }
}