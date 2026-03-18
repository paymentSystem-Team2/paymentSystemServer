package sparta.paymentsystemserver.domain.payment.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 결제 도메인 전용 예외의 부모 클래스
// 결제 생성, 결제 확정, 환불, 웹훅 처리 중 발생하는 예외를
// ErrorCode 기반으로 일관되게 응답하기 위해 사용합니다
@Getter
public class PaymentException extends RuntimeException {

    private final ErrorCode errorCode;

    public PaymentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}