package sparta.paymentsystemserver.domain.user.exception;


import sparta.paymentsystemserver.global.exception.ErrorCode;

// 이메일 중복 시 발생하는 예외 (회원가입 시 이미 존재하는 이메일로 가입 시도할 때)
public class DuplicateEmailException extends UserException {
    public DuplicateEmailException(ErrorCode errorCode) {
        super(errorCode);
    }
}