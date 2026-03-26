package sparta.paymentsystemserver.domain.order.exception;

import sparta.paymentsystemserver.global.exception.BaseException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

public class OrderException extends BaseException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
