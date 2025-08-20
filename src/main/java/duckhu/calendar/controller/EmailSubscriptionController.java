package duckhu.calendar.controller;

import duckhu.calendar.entity.EmailSubscription;
import duckhu.calendar.service.EmailSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 이메일 구독 컨트롤러
 */
@RestController
@RequestMapping("/api/email-subscriptions")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EmailSubscriptionController {

    @Autowired
    private EmailSubscriptionService subscriptionService;

    /**
     * 이메일 구독 신청
     * POST /api/email-subscriptions
     */
    @PostMapping
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> subscriptionData) {
        try {
            String email = (String) subscriptionData.get("email");
            String name = (String) subscriptionData.get("name");

            if (email == null || email.trim().isEmpty()) {
                return createErrorResponse("이메일을 입력해주세요.", HttpStatus.BAD_REQUEST);
            }

            EmailSubscription subscription = subscriptionService.subscribe(email, name);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "이메일 구독이 완료되었습니다.");
            response.put("email", subscription.getEmail());
            response.put("subscriptionId", subscription.getId());
            response.put("subscribedAt", subscription.getSubscribedAt());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("이미 구독")) {
                return createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
            }
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("구독 신청에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 구독 해지
     * POST /api/email-subscriptions/unsubscribe/{token}
     */
    @PostMapping("/unsubscribe/{token}")
    public ResponseEntity<?> unsubscribe(@PathVariable String token) {
        try {
            subscriptionService.unsubscribe(token);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "구독이 해지되었습니다.");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("구독 해지에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 구독자 목록 조회 (관리자 전용)
     * GET /api/email-subscriptions
     */
    @GetMapping("/admin")
    public ResponseEntity<?> getSubscribersAdmin() {
        try {
            List<EmailSubscription> subscribers = subscriptionService.getAllSubscribers();

            // null 체크 추가
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
            e.printStackTrace();  // 콘솔에 에러 출력

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
     * DELETE /api/email-subscriptions/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSubscriber(@PathVariable Long id) {
        try {
            subscriptionService.deleteSubscriber(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "구독자가 삭제되었습니다.");
            response.put("deletedId", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("구독자 삭제에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 활성 구독자 목록 조회 (관리자 전용)
     * GET /api/email-subscriptions/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getActiveSubscribers() {
        try {
            List<EmailSubscription> activeSubscribers = subscriptionService.getActiveSubscribers();

            if (activeSubscribers == null) {
                activeSubscribers = new ArrayList<>();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("subscribers", activeSubscribers);
            response.put("total", activeSubscribers.size());
            response.put("active", activeSubscribers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("subscribers", new ArrayList<>());
            response.put("total", 0);
            response.put("active", 0);
            response.put("error", e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

    /**
     * 구독자 상태 변경 (관리자 전용)
     * PATCH /api/email-subscriptions/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSubscriberStatus(@PathVariable Long id,
                                                    @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isActive = request.get("isActive");
            if (isActive == null) {
                return createErrorResponse("isActive 값이 필요합니다.", HttpStatus.BAD_REQUEST);
            }

            EmailSubscription updatedSubscriber = subscriptionService.updateSubscriberStatus(id, isActive);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "구독자 상태가 변경되었습니다.");
            response.put("subscriber", updatedSubscriber);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("구독자 상태 변경에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
