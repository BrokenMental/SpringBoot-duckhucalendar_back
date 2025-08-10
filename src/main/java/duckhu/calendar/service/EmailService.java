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
import java.util.concurrent.CompletableFuture;

/**
 * 이메일 발송 서비스
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@calendar.com}")
    private String fromEmail;

    @Value("${app.name:이벤트 캘린더}")
    private String appName;

    /**
     * 간단한 텍스트 이메일 전송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param body 본문
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("이메일 발송 성공: " + to);

        } catch (Exception e) {
            System.err.println("이메일 발송 실패: " + e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * HTML 이메일 전송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param htmlBody HTML 형식의 본문
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML 형식

            mailSender.send(message);
            System.out.println("HTML 이메일 발송 성공: " + to);

        } catch (MessagingException e) {
            System.err.println("HTML 이메일 발송 실패: " + e.getMessage());
            throw new RuntimeException("HTML 이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 비동기 이메일 전송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param body 본문
     */
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String body) {
        return CompletableFuture.runAsync(() -> {
            sendEmail(to, subject, body);
        });
    }

    /**
     * 이메일 인증 코드 전송
     * @param email 수신자 이메일
     * @return 생성된 인증 코드
     */
    public String sendVerificationCode(String email) {
        String code = generateVerificationCode();

        String subject = "[" + appName + "] 이메일 인증 코드";
        String body = String.format(
                "안녕하세요!\n\n" +
                        "이메일 인증 코드는 다음과 같습니다:\n\n" +
                        "인증 코드: %s\n\n" +
                        "이 코드는 5분간 유효합니다.\n" +
                        "본인이 요청하지 않으셨다면 이 메일을 무시해주세요.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                code, appName
        );

        sendEmail(email, subject, body);
        return code;
    }

    /**
     * 관리자 임시 비밀번호 전송
     * @param email 관리자 이메일
     * @return 생성된 임시 비밀번호
     */
    public String sendTempPassword(String email) {
        String tempPassword = generateTempPassword();

        String subject = "[" + appName + "] 관리자 임시 비밀번호";
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
                        "<hr>" +
                        "<p style='color: #666; font-size: 12px;'>본인이 요청하지 않으셨다면 이 메일을 무시하고 관리자에게 문의해주세요.</p>" +
                        "</body></html>",
                tempPassword
        );

        sendHtmlEmail(email, subject, htmlBody);
        return tempPassword;
    }

    /**
     * 주간 이벤트 알림 이메일 전송
     * @param to 수신자 이메일
     * @param subscriberName 구독자 이름
     * @param eventsHtml 이벤트 목록 HTML
     * @param unsubscribeToken 구독 해지 토큰
     */
    public void sendWeeklyNewsletter(String to, String subscriberName, String eventsHtml, String unsubscribeToken) {
        String subject = "[" + appName + "] 이번 주 이벤트 알림 📅";

        String greeting = subscriberName != null && !subscriberName.isEmpty()
                ? subscriberName + "님, 안녕하세요!"
                : "안녕하세요!";

        String htmlBody = String.format(
                "<html><body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                        "<h1 style='color: #333; border-bottom: 2px solid #667eea; padding-bottom: 10px;'>%s</h1>" +
                        "<p style='color: #666; font-size: 16px;'>%s</p>" +
                        "<p style='color: #666;'>이번 주 예정된 이벤트를 안내드립니다.</p>" +
                        "<div style='margin: 30px 0;'>%s</div>" +
                        "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;'>" +
                        "<p style='color: #999; font-size: 12px; text-align: center;'>" +
                        "이 메일은 %s 이벤트 알림 서비스입니다.<br>" +
                        "<a href='http://localhost:8080/unsubscribe/%s' style='color: #667eea;'>구독 해지</a>" +
                        "</p>" +
                        "</div>" +
                        "</body></html>",
                appName + " 주간 소식",
                greeting,
                eventsHtml,
                appName,
                unsubscribeToken
        );

        sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * 이벤트 요청 처리 결과 알림
     * @param to 수신자 이메일
     * @param eventTitle 이벤트 제목
     * @param isApproved 승인 여부
     */
    public void sendRequestResultEmail(String to, String eventTitle, boolean isApproved) {
        String subject = "[" + appName + "] 이벤트 요청 처리 결과";

        String result = isApproved ? "승인" : "거절";
        String message = isApproved
                ? "귀하의 이벤트가 승인되어 캘린더에 등록되었습니다."
                : "죄송합니다. 귀하의 이벤트 요청이 거절되었습니다.";

        String body = String.format(
                "안녕하세요,\n\n" +
                        "요청하신 이벤트 \"%s\"에 대한 처리 결과를 안내드립니다.\n\n" +
                        "처리 결과: %s\n\n" +
                        "%s\n\n" +
                        "추가 문의사항이 있으시면 관리자에게 문의해주세요.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                eventTitle, result, message, appName
        );

        sendEmail(to, subject, body);
    }

    /**
     * 6자리 인증 코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
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
