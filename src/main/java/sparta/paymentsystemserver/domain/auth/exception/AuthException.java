package sparta.paymentsystemserver.domain.auth.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

@Getter
public abstract class AuthException extends RuntimeException {
    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
