package sparta.paymentsystemserver.domain.point.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class InvalidPointException extends PointException {
    public InvalidPointException(ErrorCode errorCode) {
        super(errorCode);
    }
}
