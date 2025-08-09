package duckhu.calendar.config;

import duckhu.calendar.service.AdminService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JWT 인증 필터 (수정된 버전)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminService adminService;

    // 인증이 필요 없는 경로들
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/admin/request-temp-password",
            "/api/admin/login",
            "/api/schedules",
            "/api/holidays",
            "/api/event-requests",
            "/api/email-subscriptions"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // CORS preflight 요청은 바로 통과
        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // GET 요청이고 제외 경로에 포함되면 인증 없이 통과
        if ("GET".equals(method) && isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // POST /api/schedules/*/view 요청은 인증 없이 통과
        if ("POST".equals(method) && requestPath.matches(".*/api/schedules/.+/view")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token);

                    // 관리자 이메일인지 확인
                    if (adminService.isValidAdmin(email)) {
                        // 인증 정보 설정
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // 토큰 파싱 실패 시 로그만 남기고 계속 진행
                logger.debug("JWT 토큰 파싱 실패: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 제외 경로 확인
     */
    private boolean isExcludedPath(String requestPath) {
        return EXCLUDED_PATHS.stream()
                .anyMatch(excluded -> requestPath.startsWith(excluded) || requestPath.contains(excluded));
    }
}
