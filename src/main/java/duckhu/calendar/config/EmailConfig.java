package duckhu.calendar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Objects;
import java.util.Properties;

@Configuration
public class EmailConfig {

    private final Environment environment;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    public EmailConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 프로파일에 따라 동적으로 JavaMailSender 생성
     * - prod 환경: application-prod.yml의 설정 사용
     * - 그 외: null 반환하여 콘솔 모드로 동작
     */
    @Bean
    public JavaMailSender javaMailSender() {

        // prod 프로파일이 아니면 null 반환 (콘솔 모드)
        if (!"prod".equals(activeProfile)) {
            System.out.println("📧 [" + activeProfile.toUpperCase() + " MODE] 이메일은 콘솔로 출력됩니다.");
            return null;
        }

        // prod 환경에서만 실제 메일 설정 적용
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

            // application-prod.yml에서 설정값 가져오기
            String host = environment.getProperty("spring.mail.host");
            String portStr = environment.getProperty("spring.mail.port");
            String username = environment.getProperty("spring.mail.username");
            String password = environment.getProperty("spring.mail.password");

            // 필수 설정값 검증
            if (isAnyEmpty(host, portStr, username, password)) {
                System.err.println("❌ [PROD MODE] 메일 설정이 불완전합니다. 콘솔 모드로 전환합니다.");
                return null;
            }

            // 메일 센더 설정
            mailSender.setHost(host);
            mailSender.setPort(Integer.parseInt(Objects.requireNonNull(portStr)));
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            // SMTP 속성 설정
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
            props.put("mail.debug", "false");

            System.out.println("✅ [PROD MODE] 이메일 설정 완료: " + host + ":" + portStr + " (사용자: " + username + ")");
            return mailSender;

        } catch (Exception e) {
            System.err.println("❌ [PROD MODE] 이메일 설정 실패: " + e.getMessage());
            System.err.println("🔄 콘솔 모드로 전환합니다.");
            return null;
        }
    }

    /**
     * 현재 환경 정보 제공 빈
     */
    @Bean
    public EmailEnvironmentInfo emailEnvironmentInfo() {
        return new EmailEnvironmentInfo(activeProfile, javaMailSender() != null);
    }

    /**
     * 문자열이 비어있는지 확인하는 유틸리티 메서드
     */
    private boolean isAnyEmpty(String... strings) {
        for (String str : strings) {
            if (str == null || str.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 이메일 환경 정보를 담는 클래스
     */
    public static class EmailEnvironmentInfo {
        private final String profile;
        private final boolean mailEnabled;

        public EmailEnvironmentInfo(String profile, boolean mailEnabled) {
            this.profile = profile;
            this.mailEnabled = mailEnabled;
        }

        public String getProfile() {
            return profile;
        }

        public boolean isMailEnabled() {
            return mailEnabled;
        }

        public boolean isProdEnvironment() {
            return "prod".equals(profile);
        }

        public boolean isLocalEnvironment() {
            return "local".equals(profile) || "dev".equals(profile);
        }
    }
}
