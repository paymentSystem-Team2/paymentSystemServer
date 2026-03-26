package sparta.paymentsystemserver.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sparta.paymentsystemserver.global.interceptor.AdminInterceptor;

// AdminInterceptor를 /api/admin/
// 단, 관리자 로그인(/api/auth/admin/login)은 제외 (로그인 전이므로)
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                // /api/admin/** 경로에 적용
                // 인터셉터가 동작할 경로 지정
                .addPathPatterns("/api/orders/admin")
                // 관리자 로그인은 제외 (로그인하지 않은 사용자가 접근)
                // 인터셉터가 도작하지 않아야 할 경로 제외
                .excludePathPatterns("/api/auth/admin/login");
    }
}
