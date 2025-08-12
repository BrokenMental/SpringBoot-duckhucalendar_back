package duckhu.calendar.service;

import duckhu.calendar.config.EmailConfig;
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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private EmailConfig.EmailEnvironmentInfo environmentInfo;

    @Value("${spring.mail.username:noreply@calendar.com}")
    private String fromEmail;

    @Value("${app.name:더쿠 캘린더}")
    private String appName;

    /**
     * 현재 환경이 로컬/개발 환경인지 확인
     */
    private boolean isLocalEnvironment() {
        return environmentInfo.isLocalEnvironment() || mailSender == null;
    }

    /**
     * 메일 전송 가능 여부 확인
     */
    private boolean isMailAvailable() {
        return environmentInfo.isProdEnvironment() && mailSender != null;
    }

    /**
     * 환경 정보 로깅
     */
    private void logEnvironmentInfo(String operation) {
        String mode = isLocalEnvironment() ? "LOCAL/CONSOLE" : "PROD/EMAIL";
        System.out.println("📧 [" + mode + "] " + operation + " - Profile: " + environmentInfo.getProfile());
    }

    /**
     * 간단한 텍스트 이메일 전송
     */
    public void sendEmail(String to, String subject, String body) {
        logEnvironmentInfo("텍스트 이메일 전송");

        // 로컬/개발 환경에서는 콘솔 출력
        if (isLocalEnvironment()) {
            printEmailToConsole("텍스트 이메일", to, subject, body);
            return;
        }

        // 실서버 환경에서 실제 이메일 발송
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("✅ [PROD] 이메일 발송 성공: " + to);

        } catch (Exception e) {
            System.err.println("❌ [PROD] 이메일 발송 실패: " + e.getMessage());
            // 실서버에서도 실패시 콘솔로 대체
            printEmailToConsole("텍스트 이메일 (발송실패-콘솔대체)", to, subject, body);
        }
    }

    /**
     * HTML 이메일 전송
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        logEnvironmentInfo("HTML 이메일 전송");

        // 로컬/개발 환경에서는 콘솔 출력
        if (isLocalEnvironment()) {
            printEmailToConsole("HTML 이메일", to, subject, htmlBody.replaceAll("<[^>]*>", ""));
            return;
        }

        // 실서버 환경에서 실제 이메일 발송
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("✅ [PROD] HTML 이메일 발송 성공: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ [PROD] HTML 이메일 발송 실패: " + e.getMessage());
            // 실서버에서도 실패시 콘솔로 대체
            printEmailToConsole("HTML 이메일 (발송실패-콘솔대체)", to, subject, htmlBody.replaceAll("<[^>]*>", ""));
        }
    }

    /**
     * 관리자 임시 비밀번호 전송
     */
    public String sendTempPassword(String email) {
        String tempPassword = generateTempPassword();
        String subject = "[" + appName + "] 관리자 임시 비밀번호";

        logEnvironmentInfo("관리자 임시 비밀번호 발송");

        // 로컬/개발 환경에서는 특별한 형식으로 출력
        if (isLocalEnvironment()) {
            printTempPasswordToConsole(email, tempPassword);
            return tempPassword;
        }

        // 실서버에서는 이메일 발송
        String htmlBody = createTempPasswordHtml(tempPassword);
        sendHtmlEmail(email, subject, htmlBody);
        return tempPassword;
    }

    /**
     * 이메일 인증 코드 전송
     */
    public String sendVerificationCode(String email) {
        String code = generateVerificationCode();
        String subject = "[" + appName + "] 이메일 인증 코드";

        logEnvironmentInfo("이메일 인증 코드 발송");

        if (isLocalEnvironment()) {
            printVerificationCodeToConsole(email, code);
            return code;
        }

        // 실서버에서는 이메일 발송
        String htmlBody = createVerificationCodeHtml(code);
        sendHtmlEmail(email, subject, htmlBody);
        return code;
    }

    /**
     * 뉴스레터 발송
     */
    public void sendNewsletter(String email, String subject, String content) {
        logEnvironmentInfo("뉴스레터 발송");

        if (isLocalEnvironment()) {
            printEmailToConsole("뉴스레터", email, subject, content);
            return;
        }

        String htmlBody = createNewsletterHtml(content);
        sendHtmlEmail(email, subject, htmlBody);
    }

    // ========== 콘솔 출력 메서드들 ==========

    /**
     * 일반 이메일 콘솔 출력
     */
    private void printEmailToConsole(String type, String to, String subject, String body) {
        String envLabel = "[" + environmentInfo.getProfile().toUpperCase() + " MODE]";

        System.out.println("\n========================================");
        System.out.println("📧 " + envLabel + " " + type + " (콘솔 출력)");
        System.out.println("========================================");
        System.out.println("프로파일: " + environmentInfo.getProfile());
        System.out.println("메일 활성화: " + (environmentInfo.isMailEnabled() ? "예" : "아니오"));
        System.out.println("수신자: " + to);
        System.out.println("제목: " + subject);
        System.out.println("내용:");
        System.out.println(body);
        System.out.println("========================================\n");
    }

    /**
     * 임시 비밀번호 콘솔 출력
     */
    private void printTempPasswordToConsole(String email, String tempPassword) {
        String profile = environmentInfo.getProfile().toUpperCase();

        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║     🔐 관리자 임시 비밀번호 발급       ║");
        System.out.println("║         [" + String.format("%-4s", profile) + " MODE]                   ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║ 이메일: " + String.format("%-31s", email) + "║");
        System.out.println("║ 임시 비밀번호: " + String.format("%-24s", tempPassword) + "║");
        System.out.println("║ 유효시간: 30분                         ║");
        System.out.println("║ 실제 발송 여부: " + String.format("%-22s", environmentInfo.isMailEnabled() ? "실제 이메일 발송됨" : "콘솔 출력만") + "║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("\n");
    }

    /**
     * 인증 코드 콘솔 출력
     */
    private void printVerificationCodeToConsole(String email, String code) {
        String profile = environmentInfo.getProfile().toUpperCase();

        System.out.println("\n");
        System.out.println("┌─────────────────────────────────┐");
        System.out.println("│   📮 이메일 인증 코드 발송      │");
        System.out.println("│      [" + String.format("%-4s", profile) + " MODE]               │");
        System.out.println("├─────────────────────────────────┤");
        System.out.println("│ 수신자: " + String.format("%-20s", email) + "│");
        System.out.println("│ 인증코드: " + String.format("%-18s", code) + "│");
        System.out.println("│ 유효시간: 10분                  │");
        System.out.println("│ 발송방식: " + String.format("%-18s", environmentInfo.isMailEnabled() ? "실제 이메일" : "콘솔 출력") + "│");
        System.out.println("└─────────────────────────────────┘");
        System.out.println("\n");
    }

    // ========== HTML 생성 메서드들 ==========

    private String createTempPasswordHtml(String tempPassword) {
        return String.format(
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
    }

    private String createVerificationCodeHtml(String code) {
        return String.format(
                "<html><body>" +
                        "<h2>이메일 인증 안내</h2>" +
                        "<p>안녕하세요.</p>" +
                        "<p>이메일 인증을 위한 인증 코드를 안내드립니다:</p>" +
                        "<div style='background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0; text-align: center;'>" +
                        "<strong style='font-size: 24px; color: #1976d2; letter-spacing: 3px;'>%s</strong>" +
                        "</div>" +
                        "<p>위 인증 코드를 입력하여 인증을 완료해주세요.</p>" +
                        "<p>이 인증 코드는 10분간 유효합니다.</p>" +
                        "</body></html>",
                code
        );
    }

    private String createNewsletterHtml(String content) {
        return String.format(
                "<html><body>" +
                        "<div style='max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif;'>" +
                        "<h1 style='color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px;'>%s 주간 뉴스레터</h1>" +
                        "%s" +
                        "<hr style='margin: 30px 0; border: 1px solid #eee;'>" +
                        "<p style='color: #666; font-size: 12px;'>본 메일은 발신전용입니다. 수신을 원하지 않으시면 구독을 취소해주세요.</p>" +
                        "</div>" +
                        "</body></html>",
                appName, content
        );
    }

    // ========== 유틸리티 메서드들 ==========

    private String generateTempPassword() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 메일 시스템 상태 확인
     */
    public boolean isEmailSystemAvailable() {
        return isMailAvailable();
    }

    /**
     * 현재 환경 정보 반환
     */
    public EmailConfig.EmailEnvironmentInfo getEnvironmentInfo() {
        return environmentInfo;
    }

    /**
     * 이메일 시스템 상태 요약
     */
    public String getEmailSystemStatus() {
        return String.format(
                "프로파일: %s, 메일 활성화: %s, 발송 방식: %s",
                environmentInfo.getProfile(),
                environmentInfo.isMailEnabled() ? "예" : "아니오",
                isLocalEnvironment() ? "콘솔 출력" : "실제 이메일"
        );
    }
}
