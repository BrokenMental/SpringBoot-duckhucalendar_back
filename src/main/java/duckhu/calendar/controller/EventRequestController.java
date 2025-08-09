package duckhu.calendar.controller;

import duckhu.calendar.entity.EventRequest;
import duckhu.calendar.service.EventRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/event-requests")
@CrossOrigin(origins = "*")
public class EventRequestController {

    @Autowired
    private EventRequestService eventRequestService;

    /**
     * 이벤트 요청 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitEventRequest(@RequestBody EventRequest eventRequest) {
        try {
            EventRequest savedRequest = eventRequestService.submitRequest(eventRequest);
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("요청 제출 실패: " + e.getMessage());
        }
    }

    /**
     * 이메일 인증 코드 전송
     */
    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
            }

            eventRequestService.sendVerificationCode(email);
            return ResponseEntity.ok("인증 코드가 전송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("인증 코드 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 이메일 인증 확인
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");

            if (email == null || code == null) {
                return ResponseEntity.badRequest().body("이메일과 인증 코드를 모두 입력해주세요.");
            }

            boolean isValid = eventRequestService.verifyEmail(email, code);
            if (isValid) {
                return ResponseEntity.ok("인증 성공");
            } else {
                return ResponseEntity.badRequest().body("잘못된 인증 코드입니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("인증 실패: " + e.getMessage());
        }
    }

    /**
     * 관리자용 - 요청 목록 조회
     */
    @GetMapping("/admin/list")
    public ResponseEntity<List<EventRequest>> getEventRequests() {
        List<EventRequest> requests = eventRequestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * 관리자용 - 요청 승인/거절
     */
    @PutMapping("/admin/{id}/status")
    public ResponseEntity<?> updateRequestStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            eventRequestService.updateRequestStatus(id, status);
            return ResponseEntity.ok("상태가 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상태 업데이트 실패: " + e.getMessage());
        }
    }
}
