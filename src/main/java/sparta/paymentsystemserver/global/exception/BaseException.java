package sparta.paymentsystemserver.global.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException{
    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
