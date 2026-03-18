package sparta.paymentsystemserver.domain.auth.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
