package sparta.paymentsystemserver.domain.point.exception;

import sparta.paymentsystemserver.global.exception.BaseException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

public class PointException extends BaseException {

  public PointException(ErrorCode errorCode) {
    super(errorCode);
  }
}
