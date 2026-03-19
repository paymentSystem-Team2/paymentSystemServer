package sparta.paymentsystemserver.domain.order.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

@Getter
public class OrderException extends RuntimeException {

    private final ErrorCode errorCode;

    public OrderException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
