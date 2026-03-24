package sparta.paymentsystemserver.domain.subscription.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 구독 도메인 전용 예외 클래스
@Getter
public class SubscriptionException extends RuntimeException {

    private final ErrorCode errorCode;

    public SubscriptionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
