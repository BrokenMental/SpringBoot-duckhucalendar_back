package duckhu.calendar.controller;

import duckhu.calendar.config.security.AdminAuthUtil;
import duckhu.calendar.entity.EventRequest;
import duckhu.calendar.service.EventRequestService;
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
@RequestMapping("/api/event-requests")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EventRequestController {

    private final EventRequestService eventRequestService;
    private final AdminAuthUtil adminAuthUtil;

    /**
     * 이벤트 요청 목록 조회 (관리자 전용)
     */
    @GetMapping("/admin/list")
    public ResponseEntity<?> getEventRequests(HttpServletRequest request) {
        // JWT 토큰 검증
        if (!adminAuthUtil.isAdminAuthenticated(request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "접근 권한이 없습니다."
            ));
        }

        try {
            List<EventRequest> requests = eventRequestService.getAllRequests();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requests", requests != null ? requests : new ArrayList<>());
            response.put("total", requests != null ? requests.size() : 0);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이벤트 요청 목록 조회 실패: ", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "requests", new ArrayList<>(),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 요청 상태 업데이트 (관리자 전용)
     */
    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        if (!adminAuthUtil.isAdminAuthenticated(request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "접근 권한이 없습니다."
            ));
        }

        try {
            String status = body.get("status");
            eventRequestService.updateRequestStatus(id, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "요청 상태가 업데이트되었습니다."
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
     * 이벤트 요청 제출 (일반 사용자)
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitRequest(@RequestBody EventRequest eventRequest) {
        try {
            EventRequest saved = eventRequestService.submitRequest(eventRequest);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "request", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 이메일 인증 코드 전송
     */
    @PostMapping("/verify/send")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            eventRequestService.sendVerificationCode(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증 코드가 발송되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
