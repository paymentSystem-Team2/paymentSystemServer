package sparta.paymentsystemserver.domain.membership.exception;

import sparta.paymentsystemserver.global.exception.ErrorCode;

public class MembershipNotFoundException extends MembershipException {
    public MembershipNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
