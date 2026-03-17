package sparta.paymentsystemserver.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

//    공통 예외 코드 (COM###)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COM001", "입력값이 올바르지 않습니다."),

//    인증/인가 예외 (AUTH###)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "접근 권한이 없습니다.");

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
