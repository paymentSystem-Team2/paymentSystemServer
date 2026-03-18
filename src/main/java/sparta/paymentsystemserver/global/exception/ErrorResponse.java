package sparta.paymentsystemserver.global.exception;

import lombok.Builder;

import java.util.List;

@Builder
public record ErrorResponse(
        String message,
        List<FieldError> fieldErrors
) {

    public static ErrorResponse from(String message) {
        return ErrorResponse.builder()
                .message(message)
                .build();
    }

    public static ErrorResponse of(String message, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .message(message)
                .fieldErrors(fieldErrors)
                .build();
    }
}