package sparta.paymentsystemserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

// Spring Session Redis 설정
// HTTP 세션을 Redis에 저장하여 분산 환경에서 세션 공유 가능
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 세션 타임아웃: 30분
public class SessionConfig {
}

// 세션 타임아웃 관련하여 논의 필요