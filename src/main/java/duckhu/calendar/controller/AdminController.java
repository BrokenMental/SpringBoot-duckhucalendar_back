package duckhu.calendar.controller;

import duckhu.calendar.service.AdminService;
import duckhu.calendar.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
}
