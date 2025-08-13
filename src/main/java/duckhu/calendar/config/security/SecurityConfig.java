package duckhu.calendar.config.security;

import org.springframework.beans.factory.annotation.Autowired;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // @PreAuthorize 활성화
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 세션 관리 - STATELESS
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 엔드포인트 (로그인 관련)
                        .requestMatchers("/api/admin/request-temp-password", "/api/admin/login").permitAll()

                        // 공개 API (인증 불필요)
                        .requestMatchers("/api/schedules/**", "/api/holidays/**").permitAll()
                        .requestMatchers("/api/notices/active", "/api/notices/{id}").permitAll()
                        .requestMatchers("/api/event-requests/submit", "/api/event-requests/send-verification", "/api/event-requests/verify-email").permitAll()
                        .requestMatchers("/api/email-subscriptions", "/api/email-subscriptions/unsubscribe/**").permitAll()

                        // 정적 리소스 및 Vue.js 라우팅 허용
                        .requestMatchers("/", "/admin", "/admin-login", "/settings").permitAll()
                        .requestMatchers("/static/**", "/assets/**", "/*.js", "/*.css", "/*.ico", "/*.png", "/*.jpg").permitAll()

                        // 관리자 권한이 필요한 API 엔드포인트
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/notices/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/event-requests/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/email-subscriptions/admin/**").hasRole("ADMIN")

                        // 나머지 요청은 허용 (Vue.js SPA 라우팅을 위해)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 구체적인 도메인 명시 (allowCredentials: true와 호환)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
