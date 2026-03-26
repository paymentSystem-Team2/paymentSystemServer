package sparta.paymentsystemserver.domain.user.exception;

import sparta.paymentsystemserver.global.exception.BaseException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

public class UserException extends BaseException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}