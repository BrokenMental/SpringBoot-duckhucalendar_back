package duckhu.calendar.service;

import duckhu.calendar.config.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminService {

    // 임시 비밀번호 저장 (실제로는 Redis 사용 권장)
    private final Map<String, String> tempPasswords = new ConcurrentHashMap<>();
    @Autowired
    private EmailService emailService;

    @Value("${app.admin.email}")
    private String adminEmail;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 임시 비밀번호 요청
     */
    public void requestTempPassword(String email) {
        // 관리자 이메일 확인 (실제로는 DB에서 관리자 목록 조회)
        if (!email.equals(adminEmail)) {
            throw new RuntimeException("관리자 이메일이 아닙니다.");
        }

        String tempPassword = emailService.sendTempPassword(email);
        tempPasswords.put(email, tempPassword);

        // 30분 후 자동 삭제
        new Thread(() -> {
            try {
                Thread.sleep(30 * 60 * 1000); // 30분
                tempPasswords.remove(email);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 로그인
     */
    public Map<String, String> login(String email, String tempPassword) {
        String storedPassword = tempPasswords.get(email);

        if (storedPassword == null || !storedPassword.equals(tempPassword)) {
            throw new RuntimeException("잘못된 임시 비밀번호입니다.");
        }

        // 임시 비밀번호 삭제
        tempPasswords.remove(email);

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(email, "ADMIN");

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("email", email);
        response.put("role", "ADMIN");

        return response;
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}
