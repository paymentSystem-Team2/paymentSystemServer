package sparta.paymentsystemserver.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sparta.paymentsystemserver.domain.auth.exception.AuthException;
import sparta.paymentsystemserver.domain.membership.exception.MembershipException;
import sparta.paymentsystemserver.domain.product.exception.ProductException;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.user.exception.UserException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

//    Bean Validation 예외 로직 부분
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentException(MethodArgumentNotValidException exception) {
        log.error("[API - ERROR] 발생 원인: ", exception);
        ErrorCode errorcode = ErrorCode.INVALID_INPUT_VALUE;

        List<FieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::from)
                .toList();

        return ResponseEntity.status(errorcode.getStatus())
                .body(ApiResponse.fail(errorcode, fieldErrors));
    }

//    인증/인가 예외 로직 부분

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthException(AuthException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    //    사용자 예외 로직 부분
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUserException(UserException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

//    상품 예외 로직 부분

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthException(ProductException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }
//    주문 예외 로직 부분


//    결제 예외 로직 부분

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handlePaymentException(PaymentException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }


//    포인트 예외 로직 부분


//    멤버십 예외 로직 부분
    @ExceptionHandler(MembershipException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMembershipException(MembershipException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.fail(errorCode));
}
}
