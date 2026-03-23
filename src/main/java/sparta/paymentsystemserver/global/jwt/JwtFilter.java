package sparta.paymentsystemserver.global.jwt;


import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.global.redis.RedisBlackListUtil;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final RedisBlackListUtil redisBlackListUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        log.info("JWT Filter path: " + path);
        if (pathMatcher.match("/api/auth/login", path)) return true;
        if (pathMatcher.match("/api/auth/signup", path)) return true;
        if (pathMatcher.match("/api/auth/refresh", path)) return true;
        if (pathMatcher.match("/api/public/**", path)) return true;
        if (pathMatcher.match("/actuator/health", path)) return true;
        if (pathMatcher.match("/favicon.ico", path)) return true;
        if (pathMatcher.match("/api/webhooks/**", path)) return true;

        // OPTIONS preflight 제외(프론트 CORS)
        return "OPTIONS".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.error("Authorization header is missing");
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer" 제거
        String token = authorization.substring(7);
        try {
            if (jwtUtil.isExpired(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED");
                return;
            }

            if (redisBlackListUtil.isBlacklisted(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_REVOKED");
                return;
            }

            String email = jwtUtil.getEmail(token);
            Long id = jwtUtil.getId(token);

            LoginUserData loginUserData = new LoginUserData(id, email);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(loginUserData, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        }catch (ExpiredJwtException e){
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\": \"%s\"}", code));
    }
}