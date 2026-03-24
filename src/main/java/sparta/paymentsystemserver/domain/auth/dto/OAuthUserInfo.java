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
}
