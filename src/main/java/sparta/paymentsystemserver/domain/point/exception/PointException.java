package sparta.paymentsystemserver.domain.point.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

@Getter
public class PointException extends RuntimeException {
  private final ErrorCode errorCode;

  public PointException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
