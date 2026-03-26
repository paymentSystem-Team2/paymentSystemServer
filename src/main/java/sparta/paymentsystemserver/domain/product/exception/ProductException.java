package sparta.paymentsystemserver.domain.product.exception;

import sparta.paymentsystemserver.global.exception.BaseException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

public class ProductException extends BaseException {
    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}
