package sparta.paymentsystemserver.domain.subscription.exception;

import sparta.paymentsystemserver.global.exception.BaseException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 구독 도메인 전용 예외 클래스
public class SubscriptionException extends BaseException {
    public SubscriptionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
