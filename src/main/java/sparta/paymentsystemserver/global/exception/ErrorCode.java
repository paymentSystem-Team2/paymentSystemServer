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
    PASSWORD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "AUTH006", "비밀번호가 일치하지 않습니다."),

//    사용자 예외 (USER###)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "유저가 존재하지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER002", "이미 존재하는 이메일입니다."),

//    상품 예외 (PROD###)
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PROD001", "재고가 부족합니다"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST,"PROD002", "수량은 1 이상이어야 합니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROD003" , "존재하지 않는 상품 ID입니다."),

//    주문 예외 (ORD###)
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD001", "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORD002", "본인 주문만 접근할 수 있습니다."),
    ORDER_NOT_PAYABLE(HttpStatus.BAD_REQUEST, "ORD003", "결제 가능한 주문 상태가 아닙니다."),
    ORDER_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "ORD004", "주문 금액이 일치하지 않습니다."),
    ORDER_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ORD005", "주문 상태를 변경 할 수 없습니다."),

//    결제 예외 (PAY###)
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY001", "결제 정보를 찾을 수 없습니다."),
    INVALID_POINTS_TO_USE(HttpStatus.BAD_REQUEST, "PAY002", "사용 포인트 값이 올바르지 않습니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "PAY003", "보유 포인트가 부족합니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "PAY004", "PortOne 결제 검증에 실패했습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAY005", "결제 승인 금액이 서버 계산 금액과 일치하지 않습니다."),
    PAYMENT_CONFIRM_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PAY006", "확정할 수 없는 결제 상태입니다."),
    REFUND_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "PAY007", "이미 환불 완료된 결제입니다."),
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PAY008", "환불할 수 없는 결제 상태입니다."),
    REFUND_PROCESS_FAILED(HttpStatus.BAD_REQUEST, "PAY009", "PortOne 환불 처리에 실패했습니다."),

//    포인트 예외 (PNT###)
    POINT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "PNT001", "포인트 금액이 올바르지 않습니다."),
    POINT_BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "PNT002", "포인트 잔액이 부족합니다."),
    POINT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PNT003", "포인트 거래 내역을 찾을 수 없습니다."),

//    멤버십 예외 (MEM###)
    MEMBERSHIP_GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "MEM001", "멤버십 등급 정책을 찾을 수 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
