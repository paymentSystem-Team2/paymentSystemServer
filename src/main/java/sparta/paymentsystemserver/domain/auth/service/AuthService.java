package sparta.paymentsystemserver.domain.auth.service;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sparta.paymentsystemserver.domain.auth.dto.LoginRequest;
import sparta.paymentsystemserver.domain.auth.dto.LoginUser;
import sparta.paymentsystemserver.domain.auth.dto.SessionUser;
import sparta.paymentsystemserver.domain.auth.dto.SignupResponse;
import sparta.paymentsystemserver.domain.auth.dto.TokenResponse;
import sparta.paymentsystemserver.domain.auth.exception.AdminAuthorizationException;
import sparta.paymentsystemserver.domain.auth.exception.InvalidTokenException;
import sparta.paymentsystemserver.domain.auth.exception.PasswordNotFoundException;
import sparta.paymentsystemserver.domain.user.dto.UserRequest;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.jwt.JwtUtil;
import sparta.paymentsystemserver.global.redis.RedisBlackListUtil;
import sparta.paymentsystemserver.global.redis.RedisRefreshTokenUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenUtil redisRefreshTokenUtil;
    private final RedisBlackListUtil redisBlackListUtil;

    public LoginUser login(@Valid LoginRequest requestDto) {
        User user = userService.findByEmail(requestDto.getEmail());

        checkPassword(requestDto.getPassword(), user.getPassword());

        //토큰 생성
        String token = jwtUtil.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        //redis 저장
        redisRefreshTokenUtil.save(user.getId(), refreshToken, jwtUtil.getRefreshTokenExpiration());

        return new LoginUser(token,refreshToken,true, user.getEmail());
    }

    //    [Login] → Access Token (짧은 만료) + Refresh Token (긴 만료) 발급
    //    [API 요청] → Access Token으로 인증
    //    [Access Token 만료] → 클라이언트가 /api/auth/refresh 호출
    //    [Refresh Token 검증] → Redis에 저장된 값과 비교 → 새 Access Token 발급
    //    [Refresh Token 만료/없음] → 재로그인 요구
    public TokenResponse reissue(String refreshToken , String expiredAccessToken) {

        if (jwtUtil.isExpired(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }
        String accessToken = expiredAccessToken.replace("Bearer ", "");
        Long userId = jwtUtil.getId(accessToken);
        String email = jwtUtil.getEmail(accessToken);

        String savedToken = redisRefreshTokenUtil.get(userId);
        if(!refreshToken.equals(savedToken)){
            throw new InvalidTokenException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 새 Access Token 발급
        String newAccessToken = jwtUtil.createAccessToken(userId, email);

        // Refresh Token Rotation - 보안 강화
        String newRefreshToken = jwtUtil.createRefreshToken(email);
        redisRefreshTokenUtil.save(userId, newRefreshToken, jwtUtil.getRefreshTokenExpiration());
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId, String accessToken) {

        redisRefreshTokenUtil.delete(userId);
        redisBlackListUtil.save(accessToken);

    }

    //  관리자 로그인: 관리자 계정으로 세션 기반 로그인을 수행
    public SessionUser adminLogin(@Valid LoginRequest requestDto, HttpSession session) {
        // 사용자 조회
        User user = userService.findByEmail(requestDto.getEmail());

        // 비밀번호 검증
        checkPassword(requestDto.getPassword(), user.getPassword());

        // 관리자 여부 확인
        if (!user.isAdmin()) {
            throw new AdminAuthorizationException(ErrorCode.ADMIN_ONLY);
        }

        // SessionUser 생성 및 세션에 저장
        SessionUser sessionUser = new SessionUser(user);
        session.setAttribute("user", sessionUser);

        log.info("Admin login successful: {}", user.getEmail());

        return sessionUser;
    }

    public void checkPassword(String rawPassword, String password) {
        if(!passwordEncoder.matches(rawPassword, password)){
            throw new PasswordNotFoundException(ErrorCode.PASSWORD_NOT_MATCH);
        }
    }

    public SignupResponse signUp(UserRequest requestDto) {
        return userService.save(requestDto);
    }
}
