package sparta.paymentsystemserver.domain.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.exception.UserNotFoundException;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.jwt.JwtUtil;
import sparta.paymentsystemserver.global.redis.RedisRefreshTokenUtil;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisRefreshTokenUtil redisRefreshTokenUtil;

    @Value("${oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//        DefaultOAuth2User가 주입된 상태
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email;
        Object kakaoAccount = oAuth2User.getAttributes().get("kakao_account");
        if (kakaoAccount != null) {
            email = (String) ((Map<String, Object>) kakaoAccount).get("email");
        } else {
            email = (String) oAuth2User.getAttributes().get("email");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        redisRefreshTokenUtil.save(user.getId(), refreshToken, jwtUtil.getRefreshTokenExpiration());

//        프론트에서 토큰을 꺼내 저장
        String redirectUri = redirectUrl + "?access_token=" + accessToken + "&refresh_token=" + refreshToken;

        log.info("[OAuth 로그인 성공] email: {}, userId: {}", email, user.getId());

//        리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
