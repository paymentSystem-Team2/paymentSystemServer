package sparta.paymentsystemserver.domain.product.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

@Getter
public class ProductException extends RuntimeException {
    private final ErrorCode errorCode;

    public ProductException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
