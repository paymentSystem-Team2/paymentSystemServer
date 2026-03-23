package sparta.paymentsystemserver.domain.order.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class OrderInvalidStatusException extends OrderException {
    public OrderInvalidStatusException(ErrorCode errorCode) {
        super(errorCode);
    }
}
