package sparta.paymentsystemserver.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    private Api api;
    private Store store;
    private Map<String, String> channel;

    @Getter
    @Setter
    public static class Api {
        private String baseUrl;
        private String secret;
    }

    @Getter
    @Setter
    public static class Store {
        private String id;
    }
}
