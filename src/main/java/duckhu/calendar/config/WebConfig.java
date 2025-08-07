package duckhu.calendar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 클래스
 * CORS 설정 및 기타 웹 관련 설정을 담당
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:*}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    /**
     * CORS 설정
     * Vue.js 프론트엔드에서 API 접근을 허용하기 위한 설정
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // /api로 시작하는 모든 경로에 적용
                .allowedOrigins(allowedOrigins.split(",")) // 허용된 오리진 (Vue 개발 서버)
                .allowedMethods(getAllowedMethods()) // 허용된 HTTP 메서드
                .allowedHeaders(getAllowedHeaders()) // 허용된 헤더
                .allowCredentials(allowCredentials) // 인증 정보 포함 허용
                .maxAge(3600); // 프리플라이트 요청 캐시 시간 (1시간)

        // 추가적으로 루트 경로에도 CORS 적용 (헬스체크용)
        registry.addMapping("/")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET")
                .maxAge(3600);

        System.out.println("CORS 설정 완료:");
        System.out.println("- 허용된 오리진: " + allowedOrigins);
        System.out.println("- 허용된 메서드: " + allowedMethods);
        System.out.println("- 인증 정보 포함: " + allowCredentials);
    }

    /**
     * 허용된 HTTP 메서드 배열 반환
     */
    private String[] getAllowedMethods() {
        if ("*".equals(allowedMethods)) {
            return new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"};
        }
        return allowedMethods.split(",");
    }

    /**
     * 허용된 헤더 배열 반환
     */
    private String[] getAllowedHeaders() {
        if ("*".equals(allowedHeaders)) {
            return new String[]{"*"};
        }
        return allowedHeaders.split(",");
    }
}
