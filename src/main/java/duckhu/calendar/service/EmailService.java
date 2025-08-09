package duckhu.calendar.service;

import duckhu.calendar.entity.Event;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class EmailService {

    private final Random random = new SecureRandom();
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${admin.emails}")
    private String adminEmail;

    /**
     * 관리자 임시 비밀번호 발송
     */
    public String sendTempPassword(String adminEmail) {
        try {
            String tempPassword = generateTempPassword();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("[캘린더 관리자] 임시 비밀번호 발송");

            // HTML 내용 직접 작성
            String htmlContent = buildTempPasswordHtml(tempPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return tempPassword;

        } catch (MessagingException e) {
            throw new RuntimeException("임시 비밀번호 이메일 발송 실패", e);
        }
    }

    /**
     * 주간 이벤트 알림 발송
     */
    public void sendWeeklyNotification(List<String> subscriberEmails, List<Event> weeklyEvents) {
        try {
            LocalDate startOfWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            for (String email : subscriberEmails) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject(String.format("[캘린더 알림] %s ~ %s 주간 이벤트",
                        startOfWeek.format(DateTimeFormatter.ofPattern("MM/dd")),
                        endOfWeek.format(DateTimeFormatter.ofPattern("MM/dd"))));

                String htmlContent = buildWeeklyEventsHtml(weeklyEvents, startOfWeek, endOfWeek, email);
                helper.setText(htmlContent, true);

                mailSender.send(message);
            }

        } catch (MessagingException e) {
            throw new RuntimeException("주간 알림 이메일 발송 실패", e);
        }
    }

    /**
     * 이메일 인증 코드 발송
     */
    public String sendVerificationCode(String email) {
        try {
            String verificationCode = generateVerificationCode();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("[캘린더] 이메일 인증 코드");

            String htmlContent = buildVerificationCodeHtml(verificationCode);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return verificationCode;

        } catch (MessagingException e) {
            throw new RuntimeException("인증 코드 이메일 발송 실패", e);
        }
    }

    // === HTML 템플릿 메서드들 ===

    private String buildTempPasswordHtml(String tempPassword) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>임시 비밀번호</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4CAF50;">캘린더 관리자 임시 비밀번호</h2>
                    <p>안녕하세요. 요청하신 임시 비밀번호를 발송해드립니다.</p>
                    
                    <div style="background: #f8f9fa; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0;">
                        <h3 style="margin: 0; color: #4CAF50;">임시 비밀번호</h3>
                        <p style="font-size: 24px; font-weight: bold; margin: 10px 0; letter-spacing: 2px;">%s</p>
                    </div>
                    
                    <p><strong>주의사항:</strong></p>
                    <ul>
                        <li>이 비밀번호는 10분간 유효합니다.</li>
                        <li>보안을 위해 로그인 후 즉시 비밀번호를 변경해주세요.</li>
                        <li>타인에게 비밀번호를 공유하지 마세요.</li>
                    </ul>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        이 메일은 자동 발송되었습니다. 문의사항은 관리자에게 연락해주세요.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(tempPassword);
    }

    private String buildVerificationCodeHtml(String verificationCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>이메일 인증</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #007bff;">이메일 인증 코드</h2>
                    <p>캘린더 이벤트 요청을 위한 이메일 인증 코드입니다.</p>
                    
                    <div style="background: #f8f9fa; border-left: 4px solid #007bff; padding: 15px; margin: 20px 0;">
                        <h3 style="margin: 0; color: #007bff;">인증 코드</h3>
                        <p style="font-size: 32px; font-weight: bold; margin: 10px 0; letter-spacing: 3px; color: #007bff;">%s</p>
                    </div>
                    
                    <p><strong>인증 절차:</strong></p>
                    <ol>
                        <li>위의 6자리 코드를 복사하세요</li>
                        <li>요청 페이지의 인증 코드 입력란에 붙여넣기하세요</li>
                        <li>인증 확인 버튼을 클릭하세요</li>
                    </ol>
                    
                    <p style="color: #dc3545;"><strong>⚠️ 이 코드는 5분간만 유효합니다.</strong></p>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        본인이 요청하지 않은 인증이라면 이 메일을 무시하세요.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(verificationCode);
    }

    private String buildWeeklyEventsHtml(List<Event> events, LocalDate startDate, LocalDate endDate, String subscriberEmail) {
        StringBuilder eventsHtml = new StringBuilder();

        if (events.isEmpty()) {
            eventsHtml.append("<p style='color: #666; font-style: italic;'>이번 주에는 등록된 이벤트가 없습니다.</p>");
        } else {
            eventsHtml.append("<div style='margin: 20px 0;'>");
            for (Event event : events) {
                eventsHtml.append(String.format("""
                    <div style="border: 1px solid #ddd; border-radius: 8px; padding: 15px; margin: 10px 0; background: #fff;">
                        <h4 style="margin: 0 0 10px 0; color: %s;">%s</h4>
                        <p style="margin: 5px 0; color: #666;">
                            <strong>📅 날짜:</strong> %s
                        </p>
                        <p style="margin: 5px 0; color: #333;">%s</p>
                    </div>
                    """,
                        event.getColor() != null ? event.getColor() : "#333",
                        event.getTitle(),
                        event.getEventDate().format(DateTimeFormatter.ofPattern("MM월 dd일 (E)", java.util.Locale.KOREAN)),
                        event.getDescription() != null ? event.getDescription() : ""));
            }
            eventsHtml.append("</div>");
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>주간 이벤트 알림</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4CAF50;">📅 주간 이벤트 알림</h2>
                    <p>%s ~ %s 주간 이벤트를 알려드립니다.</p>
                    
                    %s
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        구독 해지를 원하시면 관리자에게 연락해주세요.<br>
                        구독자: %s
                    </p>
                </div>
            </body>
            </html>
            """.formatted(
                startDate.format(DateTimeFormatter.ofPattern("MM월 dd일")),
                endDate.format(DateTimeFormatter.ofPattern("MM월 dd일")),
                eventsHtml.toString(),
                subscriberEmail
        );
    }

    // === 기존 유틸리티 메서드들 ===

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    private String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
}
