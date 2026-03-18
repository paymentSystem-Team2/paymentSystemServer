package sparta.paymentsystemserver.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

//    공통 예외 코드 (COM###)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COM001", "입력값이 올바르지 않습니다."),

//    인증/인가 예외 (AUTH###)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "잘못된 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "만료된 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH004", "Refresh Token이 만료되었습니다. 재로그인 해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH005", "유효하지 않은 Refresh Token입니다."),
    PASSWORD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "AUTH006", "비밀번호가 일치하지 않습니다.");

//    사용자 예외 (USER###)


//    상품 예외 (PROD###)


//    주문 예외 (ORD###)


//    결제 예외 (PAY###)


//    포인트 예외 (PNT###)


//    멤버십 예외 (MEM###)

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
