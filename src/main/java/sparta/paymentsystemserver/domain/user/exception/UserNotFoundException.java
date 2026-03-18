package sparta.paymentsystemserver.domain.user.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class UserNotFoundException extends UserException {

    public UserNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
