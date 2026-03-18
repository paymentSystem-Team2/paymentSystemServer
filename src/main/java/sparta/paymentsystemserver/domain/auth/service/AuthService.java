package sparta.paymentsystemserver.domain.auth.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sparta.paymentsystemserver.domain.auth.dto.LoginRequest;
import sparta.paymentsystemserver.domain.auth.dto.LoginUser;
import sparta.paymentsystemserver.domain.auth.dto.TokenResponse;
import sparta.paymentsystemserver.domain.auth.exception.InvalidTokenException;
import sparta.paymentsystemserver.domain.auth.exception.PasswordNotFoundException;
import sparta.paymentsystemserver.domain.user.dto.UserRequestDto;
import sparta.paymentsystemserver.domain.user.dto.UserResponseDto;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.jwt.JwtUtil;
import sparta.paymentsystemserver.global.redis.RedisRefreshTokenUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenUtil redisUtil;

    public LoginUser login(@Valid LoginRequest requestDto) {
        User user = userService.findByEmail(requestDto.getEmail());

        checkPassword(requestDto.getPassword(), user.getPassword());

        //토큰 생성
        String token = jwtUtil.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getEmail());

        //redis 저장
        redisUtil.save(user.getId(), refreshToken, jwtUtil.getRefreshTokenExpiration());

        return new LoginUser(token,refreshToken,true, user.getEmail());
    }

//    [Login] → Access Token (짧은 만료) + Refresh Token (긴 만료) 발급
//    [API 요청] → Access Token으로 인증
//    [Access Token 만료] → 클라이언트가 /api/auth/refresh 호출
//    [Refresh Token 검증] → Redis에 저장된 값과 비교 → 새 Access Token 발급
//    [Refresh Token 만료/없음] → 재로그인 요구
    public TokenResponse reissue(String refreshToken) {
        // 토큰 만료 여부 확인
        if (jwtUtil.isExpired(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        Long userId = jwtUtil.getId(refreshToken);
        String email = jwtUtil.getEmail(refreshToken);

        //  Redis에 저장된 토큰과 비교 (탈취 방지)
        String savedToken = redisUtil.get(userId);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 새 Access Token 발급
        String newAccessToken = jwtUtil.createAccessToken(userId, email);

        // Refresh Token Rotation - 보안 강화
        String newRefreshToken = jwtUtil.createRefreshToken(userId, email);
        redisUtil.save(userId, newRefreshToken, jwtUtil.getRefreshTokenExpiration());

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId) {
        redisUtil.delete(userId);
    }

    public void checkPassword(String rawPassword, String password) {
        if(!passwordEncoder.matches(rawPassword, password)){
            throw new PasswordNotFoundException(ErrorCode.PASSWORD_NOT_MATCH);
        }
    }

    public UserResponseDto signUp(UserRequestDto requestDto) {
       return userService.save(requestDto);
    }
}
