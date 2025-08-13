package duckhu.calendar.controller;

import duckhu.calendar.entity.EventRequest;
import duckhu.calendar.service.EventRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/event-requests")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EventRequestController {

    @Autowired
    private EventRequestService eventRequestService;

    /**
     * 이벤트 요청 제출
     * POST /api/event-requests/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitEventRequest(@RequestBody EventRequest eventRequest) {
        try {
            // 요청 유효성 검사
            if (eventRequest.getRequesterEmail() == null ||
                    eventRequest.getRequesterEmail().trim().isEmpty()) {
                return createErrorResponse("이메일을 입력해주세요.", HttpStatus.BAD_REQUEST);
            }

            if (eventRequest.getRequestType() == null) {
                return createErrorResponse("요청 유형을 선택해주세요.", HttpStatus.BAD_REQUEST);
            }

            EventRequest savedRequest = eventRequestService.submitRequest(eventRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "이벤트 요청이 제출되었습니다.");
            response.put("requestId", savedRequest.getId());
            response.put("status", savedRequest.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("요청 제출 실패: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 이메일 인증 코드 전송
     * POST /api/event-requests/send-verification
     */
    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return createErrorResponse("이메일을 입력해주세요.", HttpStatus.BAD_REQUEST);
            }

            eventRequestService.sendVerificationCode(email);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "인증 코드가 전송되었습니다.");
            response.put("email", email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("인증 코드 전송 실패: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 이메일 인증 확인
     * POST /api/event-requests/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");

            if (email == null || code == null) {
                return createErrorResponse("이메일과 인증 코드를 모두 입력해주세요.",
                        HttpStatus.BAD_REQUEST);
            }

            boolean isValid = eventRequestService.verifyEmail(email, code);

            if (isValid) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "인증 성공");
                response.put("verified", true);
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("잘못된 인증 코드입니다.", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            return createErrorResponse("인증 실패: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 관리자용 - 요청 목록 조회
     * GET /api/event-requests/admin/list
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEventRequests() {
        try {
            List<EventRequest> requests = eventRequestService.getAllRequests();
            return ResponseEntity.ok(requests);

        } catch (Exception e) {
            return createErrorResponse("요청 목록 조회 실패: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 관리자용 - 요청 승인/거절
     * PUT /api/event-requests/admin/{id}/status?status=APPROVED
     */
    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            // 상태 유효성 검사
            if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
                return createErrorResponse("유효하지 않은 상태입니다. (APPROVED 또는 REJECTED)",
                        HttpStatus.BAD_REQUEST);
            }

            eventRequestService.updateRequestStatus(id, status);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "요청 상태가 업데이트되었습니다.");
            response.put("requestId", id);
            response.put("status", status);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("상태 업데이트 실패: " + e.getMessage(),
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
}
