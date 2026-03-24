package sparta.paymentsystemserver.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import sparta.paymentsystemserver.global.jwt.JwtUtil;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisBlackListUtil {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "blacklist:";
    private final JwtUtil jwtUtil;

    public void save(String accessToken) {
        Date expiration = jwtUtil.getExpiration(accessToken);
        long remainingTime = expiration.getTime() - System.currentTimeMillis();

        redisTemplate.opsForValue().set(
                PREFIX + accessToken,
                "logout",
                remainingTime,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + accessToken));
    }
}
