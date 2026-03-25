package sparta.paymentsystemserver.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sparta.paymentsystemserver.domain.auth.dto.SessionUser;
import sparta.paymentsystemserver.domain.auth.exception.AdminAuthorizationException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

// 관리자 엔드포인트(/api/admin/**)에 접근하기 전에 권한을 검증하는 인터셉터

// 검증 단계:
// 1. 세션 존재 여부
// 2. 세션 내 사용자 정보 존재 여부
// 3. 사용자의 ADMIN 역할 여부
@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {       //

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        // 세션 조회 (기존 세션이 없으면 생성하지 않음)
        HttpSession session = request.getSession(false);

        // 1. 세션 확인
        if (session == null) {
            log.warn("[권한 검증 실패] 세션 없음 - URI: {}", requestUri);
            throw new AdminAuthorizationException(ErrorCode.SESSION_NOT_FOUND);
        }

        // 2. SessionUser 확인
        SessionUser user = (SessionUser) session.getAttribute("user");
        if (user == null) {
            log.warn("[권한 검증 실패] 세션에 사용자 정보 없음 - URI: {}", requestUri);
            throw new AdminAuthorizationException(ErrorCode.SESSION_USER_NOT_FOUND);
        }

        // 3. ADMIN 권한 확인
        if (!user.isAdmin()) {
            log.warn("[권한 검증 실패] 관리자 권한 필요 - 사용자: {} ({}), URI: {}",
                    user.getName(), user.getEmail(), requestUri);
            throw new AdminAuthorizationException(ErrorCode.ADMIN_ONLY);
        }

        log.info("[권한 검증 성공] 관리자 접근 - 사용자: {} ({}), URI: {}",
                user.getName(), user.getEmail(), requestUri);

        return true;
    }
}