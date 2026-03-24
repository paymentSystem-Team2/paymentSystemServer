package sparta.paymentsystemserver.domain.auth.dto;

import java.util.Map;

/**
 * 구글이 보내주는 JSON 데이터
 * {
 *   "sub"                        // 구글 고유 사용자 ID
 *   "name"
 *   "given_name"
 *   "family_name"
 *   "email"
 *   "email_verified"
 *   "picture"                    // 프로필 사진 URL
 * }
 */

public record OAuthUserInfo(
        String email,
        String name,
        String providerId
) {
    public static OAuthUserInfo from(Map<String, Object> attributes) {
        return new OAuthUserInfo(
                (String) attributes.get("email"),
                (String) attributes.get("name"),
                (String) attributes.get("sub")
        );
    }


    /**
     * 카카오가 보내주는 JSON 데이터
     * {
     *   "id"          // 카카오 고유 사용자 ID
     *   "kakao_account": {
     *     "email"
     *     "profile": {
     *       "nickname"
     *     }
     *   }
     * }
     */
    public static OAuthUserInfo of(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return fromKakao(attributes);
        }
        return from(attributes);
    }

    private static OAuthUserInfo fromKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return new OAuthUserInfo(
                (String) kakaoAccount.get("email"),
                (String) profile.get("nickname"),
                String.valueOf(attributes.get("id"))
        );

    }
}
