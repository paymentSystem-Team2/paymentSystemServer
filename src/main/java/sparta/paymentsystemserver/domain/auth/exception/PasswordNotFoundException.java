package sparta.paymentsystemserver.domain.auth.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class PasswordNotFoundException extends AuthException {
    public PasswordNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
