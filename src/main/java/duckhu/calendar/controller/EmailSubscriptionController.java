package duckhu.calendar.controller;

import duckhu.calendar.config.security.AdminAuthUtil;
import duckhu.calendar.entity.EmailSubscription;
import duckhu.calendar.service.EmailSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-subscriptions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EmailSubscriptionController {

    private final EmailSubscriptionService subscriptionService;
    private final AdminAuthUtil adminAuthUtil;

    /**
     * 구독자 목록 조회 (관리자 전용)
     */
    @GetMapping("/admin")
    public ResponseEntity<?> getSubscribersAdmin(HttpServletRequest request) {
        // JWT 토큰 검증
        if (!adminAuthUtil.isAdminAuthenticated(request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "접근 권한이 없습니다."
            ));
        }

        try {
            List<EmailSubscription> subscribers = subscriptionService.getAllSubscribers();

            if (subscribers == null) {
                subscribers = new ArrayList<>();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("subscribers", subscribers);
            response.put("total", subscribers.size());
            response.put("active", subscribers.stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                    .count());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("구독자 목록 조회 실패: ", e);

            Map<String, Object> response = new HashMap<>();
            response.put("subscribers", new ArrayList<>());
            response.put("total", 0);
            response.put("active", 0);
            response.put("error", e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

    /**
     * 구독자 삭제 (관리자 전용)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubscriber(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (!adminAuthUtil.isAdminAuthenticated(request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "접근 권한이 없습니다."
            ));
        }

        try {
            subscriptionService.deleteSubscriber(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "구독자가 삭제되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 구독자 상태 변경 (관리자 전용)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateSubscriberStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest request) {

        if (!adminAuthUtil.isAdminAuthenticated(request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "접근 권한이 없습니다."
            ));
        }

        try {
            boolean isActive = body.get("isActive");
            EmailSubscription updated = subscriptionService.updateSubscriberStatus(id, isActive);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subscriber", updated
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 일반 사용자용 메서드들 (인증 불필요)

    /**
     * 이메일 구독 신청
     */
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String name = body.get("name");

            EmailSubscription subscription = subscriptionService.subscribe(email, name);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subscription", subscription
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
