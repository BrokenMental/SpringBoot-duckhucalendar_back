package duckhu.calendar.controller;

import duckhu.calendar.service.AdminService;
import duckhu.calendar.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 인증 및 기능 컨트롤러
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    private final AdminService adminService;
    private final HolidayService holidayService;

    /**
     * 임시 비밀번호 요청
     */
    @PostMapping("/request-temp-password")
    public ResponseEntity<Map<String, Object>> requestTempPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("이메일은 필수입니다.");
            }

            adminService.requestTempPassword(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "임시 비밀번호가 이메일로 발송되었습니다.");

            log.info("임시 비밀번호 요청 성공: {}", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("임시 비밀번호 요청 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 관리자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String tempPassword = request.get("tempPassword");

            if (email == null || tempPassword == null) {
                throw new RuntimeException("이메일과 임시 비밀번호는 필수입니다.");
            }

            Map<String, String> loginResult = adminService.login(email, tempPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", loginResult.get("token"));
            response.put("email", loginResult.get("email"));
            response.put("role", loginResult.get("role"));
            response.put("message", "로그인에 성공했습니다.");

            log.info("관리자 로그인 성공: {}", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("관리자 로그인 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 토큰 검증 (프론트엔드에서 /admin/me로 호출)
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> checkAuth() {
        // JWT 필터에서 이미 검증되어 이 메서드까지 도달했다면 유효한 토큰
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authenticated", true);
        response.put("role", "ADMIN");
        response.put("message", "인증이 유효합니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // JWT는 stateless이므로 서버에서 할 일은 없음
        // 클라이언트에서 토큰을 삭제하면 됨

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃되었습니다.");

        log.info("관리자 로그아웃");

        return ResponseEntity.ok(response);
    }

    /**
     * 시스템 활동 로그 조회 (관리자 전용)
     * GET /api/admin/system-activity
     */
    @GetMapping("/system-activity")
    public ResponseEntity<Map<String, Object>> getSystemActivity(@RequestParam(defaultValue = "10") int limit) {
        try {
            // 임시로 시스템 활동 데이터 생성 (실제로는 로그 테이블에서 조회)
            List<Map<String, Object>> systemActivities = new ArrayList<>();

            // 샘플 시스템 활동 데이터
            Map<String, Object> activity1 = new HashMap<>();
            activity1.put("id", 1);
            activity1.put("type", "system_startup");
            activity1.put("title", "시스템 시작");
            activity1.put("description", "애플리케이션이 정상적으로 시작되었습니다.");
            activity1.put("timestamp", java.time.LocalDateTime.now().minusHours(1));
            systemActivities.add(activity1);

            Map<String, Object> activity2 = new HashMap<>();
            activity2.put("id", 2);
            activity2.put("type", "admin_login");
            activity2.put("title", "관리자 로그인");
            activity2.put("description", "관리자가 시스템에 로그인했습니다.");
            activity2.put("timestamp", java.time.LocalDateTime.now().minusMinutes(30));
            systemActivities.add(activity2);

            Map<String, Object> response = new HashMap<>();
            response.put("activities", systemActivities);
            response.put("count", systemActivities.size());
            response.put("limit", limit);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("시스템 활동 조회 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("activities", new ArrayList<>());
            response.put("count", 0);
            response.put("error", e.getMessage());
            response.put("success", false);

            return ResponseEntity.ok(response);
        }
    }
}
