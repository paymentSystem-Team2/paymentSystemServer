package sparta.paymentsystemserver.global.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiResponse<T> (
        boolean success,
        String code,
        T data,
        LocalDateTime timestamp
) {

    public static ApiResponse<ErrorResponse> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(),
                ErrorResponse.from(errorCode.getMessage()), LocalDateTime.now());
    }

    public static ApiResponse<ErrorResponse> fail(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, errorCode.getCode(),
                ErrorResponse.of(errorCode.getMessage(), fieldErrors), LocalDateTime.now());
    }
}
