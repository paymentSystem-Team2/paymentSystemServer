package sparta.paymentsystemserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// 외부 http api 호풀에 사용할 빈 설정
// 포트원 같은 외부 REST API 연동 때 공통으로 일단 재사용할 수 있도록 등록
@Configuration
public class RestClientConfig {

    // RestClient.Builder 빈 등록 PortOnePaymentClientImpl에서 생성자 주입으로 받아서 사용
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
