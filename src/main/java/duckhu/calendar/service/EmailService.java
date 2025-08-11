package duckhu.calendar.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class EmailService {

    @Autowired(required = false)  // 이메일 설정이 없어도 실행 가능
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@calendar.com}")
    private String fromEmail;

    @Value("${app.name:더쿠 캘린더}")
    private String appName;

    @Value("${app.admin.console-output:true}")  // 기본값 true (로컬)
    private boolean consoleOutput;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * 간단한 텍스트 이메일 전송
     */
    public void sendEmail(String to, String subject, String body) {
        // 로컬 환경이거나 콘솔 출력 설정이 true면 콘솔에 출력
        if (isLocalEnvironment()) {
            System.out.println("========================================");
            System.out.println("📧 [LOCAL MODE] 이메일 전송 (콘솔 출력)");
            System.out.println("========================================");
            System.out.println("수신자: " + to);
            System.out.println("제목: " + subject);
            System.out.println("내용:");
            System.out.println(body);
            System.out.println("========================================");
            return;
        }

        // 실서버 환경에서는 실제 이메일 발송
        try {
            if (mailSender == null) {
                throw new RuntimeException("이메일 설정이 되어있지 않습니다.");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("✅ 이메일 발송 성공: " + to);

        } catch (Exception e) {
            System.err.println("❌ 이메일 발송 실패: " + e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * HTML 이메일 전송
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        // 로컬 환경에서는 콘솔 출력
        if (isLocalEnvironment()) {
            System.out.println("========================================");
            System.out.println("📧 [LOCAL MODE] HTML 이메일 (콘솔 출력)");
            System.out.println("========================================");
            System.out.println("수신자: " + to);
            System.out.println("제목: " + subject);
            System.out.println("HTML 내용:");
            System.out.println(htmlBody.replaceAll("<[^>]*>", "")); // HTML 태그 제거
            System.out.println("========================================");
            return;
        }

        // 실서버 환경
        try {
            if (mailSender == null) {
                throw new RuntimeException("이메일 설정이 되어있지 않습니다.");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("✅ HTML 이메일 발송 성공: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ HTML 이메일 발송 실패: " + e.getMessage());
            throw new RuntimeException("HTML 이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 관리자 임시 비밀번호 전송
     */
    public String sendTempPassword(String email) {
        String tempPassword = generateTempPassword();

        String subject = "[" + appName + "] 관리자 임시 비밀번호";

        // 로컬 환경에서는 특별한 형식으로 출력
        if (isLocalEnvironment()) {
            System.out.println("\n");
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║     🔐 관리자 임시 비밀번호 발급       ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ 이메일: " + String.format("%-31s", email) + "║");
            System.out.println("║ 임시 비밀번호: " + String.format("%-24s", tempPassword) + "║");
            System.out.println("║ 유효시간: 30분                         ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("\n");
            return tempPassword;
        }

        // 실서버에서는 이메일 발송
        String htmlBody = String.format(
                "<html><body>" +
                        "<h2>관리자 임시 비밀번호 안내</h2>" +
                        "<p>안녕하세요, 관리자님.</p>" +
                        "<p>요청하신 임시 비밀번호를 안내드립니다:</p>" +
                        "<div style='background-color: #f4f4f4; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                        "<strong style='font-size: 18px; color: #333;'>%s</strong>" +
                        "</div>" +
                        "<p>보안을 위해 로그인 후 즉시 비밀번호를 변경해주세요.</p>" +
                        "<p>이 임시 비밀번호는 30분간 유효합니다.</p>" +
                        "</body></html>",
                tempPassword
        );

        sendHtmlEmail(email, subject, htmlBody);
        return tempPassword;
    }

    /**
     * 이메일 인증 코드 전송
     */
    public String sendVerificationCode(String email) {
        String code = generateVerificationCode();

        String subject = "[" + appName + "] 이메일 인증 코드";

        // 로컬 환경
        if (isLocalEnvironment()) {
            System.out.println("\n");
            System.out.println("┌─────────────────────────────────┐");
            System.out.println("│   📮 이메일 인증 코드 발송      │");
            System.out.println("├─────────────────────────────────┤");
            System.out.println("│ 수신자: " + String.format("%-24s", email) + "│");
            System.out.println("│ 인증코드: " + String.format("%-22s", code) + "│");
            System.out.println("│ 유효시간: 5분                   │");
            System.out.println("└─────────────────────────────────┘");
            System.out.println("\n");
            return code;
        }

        // 실서버
        String body = String.format(
                "안녕하세요!\n\n" +
                        "이메일 인증 코드는 다음과 같습니다:\n\n" +
                        "인증 코드: %s\n\n" +
                        "이 코드는 5분간 유효합니다.\n",
                code
        );

        sendEmail(email, subject, body);
        return code;
    }

    /**
     * 로컬 환경인지 확인
     */
    private boolean isLocalEnvironment() {
        return consoleOutput || "local".equals(activeProfile) || "dev".equals(activeProfile);
    }

    /**
     * 6자리 인증 코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 임시 비밀번호 생성 (8자리)
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}
