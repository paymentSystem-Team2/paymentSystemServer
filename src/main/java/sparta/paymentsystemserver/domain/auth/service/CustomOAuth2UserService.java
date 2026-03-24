package sparta.paymentsystemserver.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.auth.dto.OAuthUserInfo;
import sparta.paymentsystemserver.domain.user.entity.AuthProvider;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.util.Collections;

// Spring Security가 구글 인증 완료 후 자동으로 이 메서드를 호출
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PublicIdGenerator publicIdGenerator;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
//        Spring이 기본으로 제공하는 구글 정보 조회 클래스
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

//        구글 서버에 요청해서 사용자 정보(JSON) 가져오는 것
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

//        구글인지, 카카오인지 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuthUserInfo userInfo = OAuthUserInfo.of(registrationId, oAuth2User.getAttributes());

        AuthProvider provider = "kakao".equals(registrationId)
                ? AuthProvider.KAKAO
                : AuthProvider.GOOGLE;

        userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> registerNewUser(userInfo, provider));

        log.info("[OAuth 로그인] provider: {}, email: {}", registrationId, userInfo.email());

//        구글: "email", 카카오: "id"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                userNameAttributeName
        );
    }

    private User registerNewUser(OAuthUserInfo userInfo, AuthProvider provider) {
        User user = new User(
                userInfo.name(),
                userInfo.email(),
                "",
                publicIdGenerator.generate("USR"),
                provider,
                userInfo.providerId()
        );

        log.info("[OAuth 신규 회원가입] provider:{}, email: {}", provider, userInfo.email());
        return userRepository.save(user);
    }
}
