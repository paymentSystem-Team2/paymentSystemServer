package sparta.paymentsystemserver.global.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 외부에 노출되는 공개 식별자를 생성하는 유틸리티
// 사람이 읽을 수 있는 규칙 유지, 충돌 가능성 낮추기 위해 시간 문자열이랑 짧은 난수 조합
@Component
public class PublicIdGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SecureRandom secureRandom = new SecureRandom();


    // 접두사 + 시각 + 난수 조합으로 공개 ID를 생성
    // 예를 들어 - USR-... ORD-... PAY-...
    public String generate(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(FORMATTER) + "-" + randomPart(10);
    }

    // 난수 구간
    private String randomPart(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
        }
        return builder.toString();
    }

}
