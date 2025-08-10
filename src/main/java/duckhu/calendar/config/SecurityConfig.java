package duckhu.calendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정 (수정된 버전)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 세션 관리 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 엔드포인트 (경로 수정)
                        .requestMatchers("GET", "/schedules", "/schedules/**").permitAll()
                        .requestMatchers("POST", "/schedules/*/view").permitAll()
                        .requestMatchers("GET", "/holidays/**").permitAll()
                        .requestMatchers("/event-requests/**").permitAll()
                        .requestMatchers("/email-subscriptions/**").permitAll()
                        .requestMatchers("/admin/request-temp-password").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/files/upload/**").permitAll()

                        // OPTIONS 요청 허용 (CORS preflight)
                        .requestMatchers("OPTIONS", "/**").permitAll()

                        // 관리자 권한 필요
                        .requestMatchers("POST", "/schedules").hasRole("ADMIN")
                        .requestMatchers("PUT", "/schedules/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/schedules/**").hasRole("ADMIN")
                        .requestMatchers("PATCH", "/schedules/**").hasRole("ADMIN")
                        .requestMatchers("POST", "/holidays/**").hasRole("ADMIN")
                        .requestMatchers("PUT", "/holidays/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/holidays/**").hasRole("ADMIN")
                        .requestMatchers("/admin/me").hasRole("ADMIN")

                        // 나머지 요청은 허용 (개발 중이므로)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 (더 구체적으로 설정)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:*",
                "https://localhost:*"
        ));

        // 허용할 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "X-Total-Count", "Content-Range"
        ));

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 CORS 설정 적용
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}
