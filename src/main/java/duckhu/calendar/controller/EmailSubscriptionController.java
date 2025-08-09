package duckhu.calendar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 이메일 구독 컨트롤러
 */
@RestController
@RequestMapping("/email-subscriptions")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EmailSubscriptionController {

    /**
     * 이메일 구독 신청
     * POST /api/email-subscriptions
     */
    @PostMapping
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> subscriptionData) {
        try {
            String email = (String) subscriptionData.get("email");
            if (email == null || email.trim().isEmpty()) {
                return createErrorResponse("이메일을 입력해주세요.");
            }

            // TODO: 실제 구독 처리 로직 구현
            // - 이메일 중복 확인
            // - 데이터베이스에 저장
            // - 구독 확인 이메일 발송

            Map<String, Object> response = new HashMap<>();
            response.put("message", "이메일 구독이 완료되었습니다.");
            response.put("email", email);
            response.put("subscriptionDate", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("구독 신청에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 구독 해지
     * POST /api/email-subscriptions/unsubscribe/{token}
     */
    @PostMapping("/unsubscribe/{token}")
    public ResponseEntity<?> unsubscribe(@PathVariable String token) {
        try {
            // TODO: 실제 구독 해지 로직 구현

            Map<String, Object> response = new HashMap<>();
            response.put("message", "구독이 해지되었습니다.");
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("구독 해지에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 구독자 목록 조회 (관리자 전용)
     * GET /api/email-subscriptions
     */
    @GetMapping
    public ResponseEntity<?> getSubscribers() {
        try {
            // TODO: 실제 구독자 목록 조회 로직 구현

            Map<String, Object> response = new HashMap<>();
            response.put("subscribers", new java.util.ArrayList<>()); // 빈 목록 반환
            response.put("total", 0);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("구독자 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private ResponseEntity<?> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
