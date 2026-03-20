package sparta.paymentsystemserver.domain.point.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class InsufficientPointException extends PointException {
    public InsufficientPointException(ErrorCode errorCode) {
        super(errorCode);
    }
}
