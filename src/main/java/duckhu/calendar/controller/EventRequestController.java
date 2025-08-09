package duckhu.calendar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 이벤트 요청 컨트롤러
 */
@RestController
@RequestMapping("/event-requests")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EventRequestController {

    /**
     * 이벤트 요청 제출
     * POST /api/event-requests
     */
    @PostMapping
    public ResponseEntity<?> submitEventRequest(@RequestBody Map<String, Object> requestData) {
        try {
            // TODO: 실제 이벤트 요청 처리 로직 구현
            // 현재는 기본 응답만 반환

            Map<String, Object> response = new HashMap<>();
            response.put("message", "이벤트 요청이 성공적으로 제출되었습니다.");
            response.put("requestId", System.currentTimeMillis()); // 임시 ID
            response.put("status", "PENDING");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("이벤트 요청 제출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 이벤트 요청 목록 조회 (관리자 전용)
     * GET /api/event-requests
     */
    @GetMapping
    public ResponseEntity<?> getEventRequests(@RequestParam(required = false) String status) {
        try {
            // TODO: 실제 이벤트 요청 목록 조회 로직 구현

            Map<String, Object> response = new HashMap<>();
            response.put("requests", new java.util.ArrayList<>()); // 빈 목록 반환
            response.put("total", 0);
            response.put("status", status);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("이벤트 요청 목록 조회에 실패했습니다: " + e.getMessage());
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
