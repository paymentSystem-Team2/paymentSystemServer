package sparta.paymentsystemserver.domain.product.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class ProductStockException extends ProductException{
    public ProductStockException (ErrorCode errorCode){super(errorCode);}
}
