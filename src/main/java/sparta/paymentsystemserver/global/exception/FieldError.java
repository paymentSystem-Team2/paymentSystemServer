package sparta.paymentsystemserver.global.exception;

public record FieldError(
        String field,
        String rejectedValue,
        String message
) {

    public static FieldError from(org.springframework.validation.FieldError error) {
        return new FieldError(
                error.getField(),
                error.getRejectedValue() != null ? error.getRejectedValue().toString() : null,
                error.getDefaultMessage()
        );
    }
}
