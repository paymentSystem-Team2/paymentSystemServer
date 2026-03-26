package sparta.paymentsystemserver.domain.membership.exception;

import sparta.paymentsystemserver.global.exception.BaseException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

public class MembershipException extends BaseException { // 등급 정책 조회 실패 등 멤버십 관련 예외를 ErrorCode 기반으로 처리
    public MembershipException(ErrorCode errorCode) {
        super(errorCode);
    }
}