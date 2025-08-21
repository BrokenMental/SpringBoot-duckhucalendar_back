package duckhu.calendar.config.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthUtil {

    @Autowired
    private JwtUtil jwtUtil;

    @PostConstruct
    public void init() {
        System.out.println("=== AdminAuthUtil 초기화 ===");
        System.out.println("JwtUtil 주입: " + (jwtUtil != null ? "성공" : "실패"));
    }

    public boolean isAdminAuthenticated(HttpServletRequest request) {
        System.out.println("\n=== AdminAuthUtil.isAdminAuthenticated 호출됨 ===");
        System.out.println("요청 URL: " + request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization 헤더: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ 토큰이 없거나 형식이 잘못됨");
            return false;
        }

        String token = authHeader.substring(7);
        System.out.println("추출된 토큰 길이: " + token.length());

        try {
            boolean isValid = jwtUtil.validateToken(token);
            System.out.println("토큰 유효성: " + isValid);

            if (!isValid) {
                System.out.println("❌ 토큰이 유효하지 않음");
                return false;
            }

            boolean isAdmin = jwtUtil.isAdminToken(token);
            System.out.println("관리자 권한: " + isAdmin);

            boolean result = isValid && isAdmin;
            System.out.println("최종 인증 결과: " + result);

            return result;
        } catch (Exception e) {
            System.out.println("❌ 토큰 검증 중 에러: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
