package sparta.paymentsystemserver.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserUpdateRequest {

    // 이름: 최대 50자 제한 (null이면 수정하지 않음)
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    // 전화번호: 숫자 10~11자리만 허용 (null이면 수정하지 않음)
    // 10자리: 0212341234, 11자리: 01012341234
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

}