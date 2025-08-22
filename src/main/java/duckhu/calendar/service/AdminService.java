package duckhu.calendar.service;

import duckhu.calendar.config.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AdminService {

    // 임시 비밀번호 생성용 문자
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동하기 쉬운 문자 제외 (I, O, 1, 0)

    @Value("${app.admin.email}")
    private String adminEmail;
    private static final int PASSWORD_LENGTH = 6;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int EXPIRY_MINUTES = 5; // 5분으로 변경
    // 임시 비밀번호 저장
    private final Map<String, TempPasswordInfo> tempPasswords = new ConcurrentHashMap<>();
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 임시 비밀번호 요청
     */
    public void requestTempPassword(String email) {
        // 관리자 이메일 확인
        if (!email.equals(adminEmail)) {
            throw new RuntimeException("등록되지 않은 관리자 이메일입니다.");
        }

        // 기존 임시 비밀번호가 있으면 삭제
        tempPasswords.remove(email);

        // 새로운 임시 비밀번호 생성 (문자+숫자 조합)
        String tempPassword = generateTempPassword();

        // 5분 후 만료
        long expiryTime = System.currentTimeMillis() + (EXPIRY_MINUTES * 60 * 1000);

        tempPasswords.put(email, new TempPasswordInfo(tempPassword, expiryTime));

        // 실제로는 이메일 발송 서비스를 사용해야 함
        // 현재는 로그로 대체 (개발용)
        log.info("=".repeat(50));
        log.info("📧 관리자 임시 비밀번호 발송");
        log.info("이메일: {}", email);
        log.info("임시 비밀번호: {}", tempPassword);
        log.info("유효 시간: {}분", EXPIRY_MINUTES);
        log.info("=".repeat(50));

        // 5분 후 자동 삭제 스케줄링
        scheduler.schedule(() -> {
            TempPasswordInfo info = tempPasswords.get(email);
            if (info != null && info.isExpired()) {
                tempPasswords.remove(email);
                log.info("임시 비밀번호 자동 만료: {}", email);
            }
        }, EXPIRY_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 6자리 임시 비밀번호 생성 (문자+숫자 조합)
     * 예: A3K7P9, B2M5Q8 등
     */
    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(index));
        }

        return password.toString();
    }

    /**
     * 로그인
     */
    public Map<String, String> login(String email, String tempPassword) {
        TempPasswordInfo storedInfo = tempPasswords.get(email);

        if (storedInfo == null) {
            throw new RuntimeException("유효하지 않은 임시 비밀번호입니다. 새로 요청해주세요.");
        }

        if (storedInfo.isExpired()) {
            tempPasswords.remove(email);
            throw new RuntimeException("임시 비밀번호가 만료되었습니다. 새로 요청해주세요.");
        }

        if (!storedInfo.password.equals(tempPassword)) {
            throw new RuntimeException("잘못된 임시 비밀번호입니다.");
        }

        // 임시 비밀번호 삭제 (일회용)
        tempPasswords.remove(email);

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(email, "ADMIN");

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("email", email);
        response.put("role", "ADMIN");

        log.info("관리자 로그인 성공: {}", email);

        return response;
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 임시 비밀번호 정보를 담는 내부 클래스
     */
    private static class TempPasswordInfo {
        final String password;
        final long expiryTime;

        TempPasswordInfo(String password, long expiryTime) {
            this.password = password;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /**
     * 현재 활성 임시 비밀번호 개수 (모니터링용)
     */
    public int getActiveTempPasswordCount() {
        // 만료된 것들 정리
        tempPasswords.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return tempPasswords.size();
    }
}
