package duckhu.calendar.service;

import duckhu.calendar.entity.EmailSubscription;
import duckhu.calendar.repository.EmailSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmailSubscriptionService {

    @Autowired
    private EmailSubscriptionRepository subscriptionRepository;

    @Autowired
    private EmailService emailService;

    /**
     * 이메일 구독 신청
     */
    @Transactional
    public EmailSubscription subscribe(String email, String name) {
        // 이미 구독중인지 확인
        if (subscriptionRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 구독중인 이메일 주소입니다.");
        }

        EmailSubscription subscription = new EmailSubscription();
        subscription.setEmail(email);
        subscription.setSubscriberName(name);
        subscription.setIsActive(true);
        subscription.setUnsubscribeToken(UUID.randomUUID().toString());

        EmailSubscription saved = subscriptionRepository.save(subscription);

        // 구독 환영 이메일 발송
        sendWelcomeEmail(saved);

        return saved;
    }

    /**
     * 구독 해지
     */
    @Transactional
    public void unsubscribe(String token) {
        EmailSubscription subscription = subscriptionRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 구독 해지 토큰입니다."));

        subscription.setIsActive(false);
        subscription.setUnsubscribedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        // 구독 해지 확인 이메일
        sendUnsubscribeConfirmation(subscription);
    }

    /**
     * 모든 구독자 조회 (관리자용)
     */
    public List<EmailSubscription> getAllSubscribers() {
        try {
            // 간단한 findAll 사용
            List<EmailSubscription> result = subscriptionRepository.findAll();

            // 정렬이 필요하면 여기서
            if (result != null && !result.isEmpty()) {
                result.sort((a, b) -> {
                    if (b.getSubscribedAt() == null) return -1;
                    if (a.getSubscribedAt() == null) return 1;
                    return b.getSubscribedAt().compareTo(a.getSubscribedAt());
                });
            }

            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("구독자 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 활성 구독자 조회
     */
    public List<EmailSubscription> getActiveSubscribers() {
        return subscriptionRepository.findByIsActiveTrue();
    }

    /**
     * 구독자 삭제 (관리자용)
     */
    @Transactional
    public void deleteSubscriber(Long subscriberId) {
        subscriptionRepository.deleteById(subscriberId);
    }

    /**
     * 구독자 상태 변경 (관리자용)
     */
    @Transactional
    public EmailSubscription updateSubscriberStatus(Long subscriberId, boolean isActive) {
        EmailSubscription subscription = subscriptionRepository.findById(subscriberId)
                .orElseThrow(() -> new RuntimeException("구독자를 찾을 수 없습니다."));

        subscription.setIsActive(isActive);
        if (!isActive) {
            subscription.setUnsubscribedAt(LocalDateTime.now());
        }
        return subscriptionRepository.save(subscription);
    }

    /**
     * 환영 이메일 발송
     */
    private void sendWelcomeEmail(EmailSubscription subscription) {
        String subject = "더쿠 캘린더 구독을 환영합니다!";
        String body = String.format(
                "안녕하세요 %s님,\n\n" +
                        "더쿠 캘린더 이메일 알림 구독을 시작하셨습니다.\n" +
                        "매주 일요일 오전 9시에 다가오는 이벤트 정보를 받아보실 수 있습니다.\n\n" +
                        "구독을 해지하시려면 다음 링크를 클릭하세요:\n" +
                        "http://localhost:8080/unsubscribe/%s\n\n" +
                        "감사합니다.",
                subscription.getSubscriberName() != null ? subscription.getSubscriberName() : "고객",
                subscription.getUnsubscribeToken()
        );

        emailService.sendEmail(subscription.getEmail(), subject, body);
    }

    /**
     * 구독 해지 확인 이메일
     */
    private void sendUnsubscribeConfirmation(EmailSubscription subscription) {
        String subject = "구독이 해지되었습니다";
        String body = "더쿠 캘린더 이메일 알림 구독이 해지되었습니다.\n" +
                "다시 구독을 원하시면 언제든지 웹사이트를 방문해주세요.";

        emailService.sendEmail(subscription.getEmail(), subject, body);
    }
}
