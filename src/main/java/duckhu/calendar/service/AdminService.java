package duckhu.calendar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 인증 및 관리 서비스
 */
@Service
public class AdminService {

    @Value("${admin.emails}")
    private String adminEmail;

    @Value("${app.dev.mode:false}")
    private boolean devMode;

    @Autowired
    private JavaMailSender mailSender;

    // 임시 비밀번호 저장소 (실제 운영에서는 Redis나 DB 사용 권장)
    private Map<String, TempPassword> tempPasswords = new HashMap<>();

    /**
     * 관리자 이메일인지 확인
     */
    public boolean isValidAdmin(String email) {
        return adminEmail.trim().equalsIgnoreCase(email.trim());
    }

    /**
     * 임시 비밀번호 생성 및 이메일 발송
     */
    public void sendTempPassword(String email) {
        if (!isValidAdmin(email)) {
            throw new RuntimeException("등록되지 않은 관리자 이메일입니다.");
        }

        // 6자리 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // 15분 후 만료
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

        // 임시 비밀번호 저장
        tempPasswords.put(email, new TempPassword(tempPassword, expiryTime));

        // 이메일 발송
        try {
            sendEmail(email, tempPassword);
        } catch (Exception e) {
            tempPasswords.remove(email); // 이메일 발송 실패 시 임시 비밀번호 제거
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 임시 비밀번호 검증
     */
    public boolean validateTempPassword(String email, String password) {
        if (!isValidAdmin(email)) {
            throw new RuntimeException("관리자 이메일이 아닙니다.");
        }

        TempPassword tempPassword = tempPasswords.get(email);

        if (tempPassword == null) {
            throw new RuntimeException("임시 비밀번호를 먼저 요청해주세요.");
        }

        if (tempPassword.isExpired()) {
            tempPasswords.remove(email); // 만료된 비밀번호 제거
            throw new RuntimeException("임시 비밀번호가 만료되었습니다.");
        }

        boolean isValid = tempPassword.password.equals(password);

        if (isValid) {
            tempPasswords.remove(email); // 사용된 비밀번호 제거 (일회용)
        }

        return isValid;
    }

    /**
     * 6자리 임시 비밀번호 생성
     */
    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        int password = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(password);
    }

    /**
     * 이메일 발송
     */
    private void sendEmail(String toEmail, String tempPassword) {
        if (devMode) {
            // 개발 모드에서는 콘솔에만 출력
            System.out.println("=== 개발 모드: 임시 비밀번호 ===");
            System.out.println("이메일: " + toEmail);
            System.out.println("임시 비밀번호: " + tempPassword);
            System.out.println("==============================");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[캘린더 관리자] 임시 비밀번호 발송");
        message.setText(String.format(
                "안녕하세요.\n\n" +
                        "요청하신 관리자 임시 비밀번호를 발송해드립니다.\n\n" +
                        "임시 비밀번호: %s\n\n" +
                        "※ 이 비밀번호는 15분 후 만료되며, 일회용입니다.\n" +
                        "※ 로그인 후 즉시 사용이 제한됩니다.\n\n" +
                        "감사합니다.",
                tempPassword
        ));

        mailSender.send(message);
    }

    /**
     * 만료된 임시 비밀번호 정리 (스케줄러로 주기적 실행 권장)
     */
    public void cleanupExpiredPasswords() {
        tempPasswords.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 임시 비밀번호 정보를 저장하는 내부 클래스
     */
    private static class TempPassword {
        String password;
        LocalDateTime expiryTime;

        TempPassword(String password, LocalDateTime expiryTime) {
            this.password = password;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}
