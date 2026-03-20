package sparta.paymentsystemserver.domain.membership.exception;

import lombok.Getter;
import sparta.paymentsystemserver.global.exception.ErrorCode;

@Getter
public class MembershipException extends RuntimeException { // 등급 정책 조회 실패 등 멤버십 관련 예외를 ErrorCode 기반으로 처리

    private final ErrorCode errorCode;

    public MembershipException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}