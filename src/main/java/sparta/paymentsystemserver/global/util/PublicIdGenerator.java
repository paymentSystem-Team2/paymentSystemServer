package sparta.paymentsystemserver.global.util;

import com.github.f4b6a3.tsid.TsidCreator;
import org.springframework.stereotype.Component;

// 외부에 노출되는 공개 식별자를 생성하는 유틸리티
// TSID 기반으로 정렬 가능한 고유 id를 만들고 도메인별로 prefix를 붙여서 사용합니다
@Component
public class PublicIdGenerator {

    public String generate(String prefix) {
        return prefix + "-" + TsidCreator.getTsid();
    }

}
