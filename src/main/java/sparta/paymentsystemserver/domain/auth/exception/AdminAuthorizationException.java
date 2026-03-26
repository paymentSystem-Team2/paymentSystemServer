package sparta.paymentsystemserver.domain.auth.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

//  관리자 권한 검증 실패 예외: 일반 사용자가 관리자 전용 기능 접근 시 발생
public class AdminAuthorizationException extends AuthException {
    public AdminAuthorizationException(ErrorCode errorCode) {
        super(errorCode);
    }
}