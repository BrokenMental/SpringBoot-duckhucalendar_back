package duckhu.calendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정 (최대 호환성 버전)
 * 모든 Spring Security 버전에서 동작하도록 단순화
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

                // 요청 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 모든 GET 요청은 허용
                        .requestMatchers("GET", "/api/schedules/**").permitAll()
                        .requestMatchers("POST", "/api/schedules/*/view").permitAll()
                        .requestMatchers("/api/event-requests/**").permitAll()
                        .requestMatchers("/api/email-subscriptions/**").permitAll()
                        .requestMatchers("/api/admin/request-temp-password").permitAll()
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/files/upload/**").permitAll()

                        // POST, PUT, DELETE, PATCH는 ADMIN 권한 필요
                        .requestMatchers("POST", "/api/schedules").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/schedules/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/schedules/**").hasRole("ADMIN")
                        .requestMatchers("PATCH", "/api/schedules/**").hasRole("ADMIN")

                        // 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "https://localhost:*"
        ));

        // 허용할 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // 캐시 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
