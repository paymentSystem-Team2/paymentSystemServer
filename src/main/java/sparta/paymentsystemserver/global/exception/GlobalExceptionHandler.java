package sparta.paymentsystemserver.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentException(Exception exception) {
        log.error("[API - ERROR] 발생 원인: ", exception);
        ErrorCode errorcode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorcode.getStatus())
                .body(ApiResponse.fail(errorcode));
    }

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

    // 하나의 공통 에러로 추상화
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

//    인증/인가 예외 로직 부분

//    @ExceptionHandler(AuthException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthException(AuthException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//
//    //    사용자 예외 로직 부분
//    @ExceptionHandler(UserException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handleUserException(UserException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//
//    상품 예외 로직 부분
//
//    @ExceptionHandler(ProductException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthException(ProductException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//    주문 예외 로직 부분
//    @ExceptionHandler(OrderException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handleOrderException(OrderException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//
//
//    결제 예외 로직 부분
//
//    @ExceptionHandler(PaymentException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handlePaymentException(PaymentException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//
//    //    구독 예외 로직 부분
//    @ExceptionHandler(SubscriptionException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handleSubscriptionException(SubscriptionException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//
//    포인트 예외 로직 부분
//    @ExceptionHandler(PointException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handlePaymentException(PointException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(ApiResponse.fail(errorCode));
//    }
//
//    멤버십 예외 로직 부분
//    @ExceptionHandler(MembershipException.class)
//    public ResponseEntity<ApiResponse<ErrorResponse>> handleMembershipException(MembershipException e) {
//        ErrorCode errorCode = e.getErrorCode();
//        return ResponseEntity
//            .status(errorCode.getStatus())
//            .body(ApiResponse.fail(errorCode));
//    }
}
