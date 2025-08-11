package duckhu.calendar.controller;

import duckhu.calendar.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 임시 비밀번호 요청
     */
    @PostMapping("/request-temp-password")
    public ResponseEntity<?> requestTempPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            adminService.requestTempPassword(email);

            return ResponseEntity.ok(Map.of(
                    "message", "임시 비밀번호가 이메일로 전송되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String tempPassword = request.get("tempPassword");

            Map<String, String> response = adminService.login(email, tempPassword);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 토큰 검증
     */
    @GetMapping("/me")
    public ResponseEntity<?> checkAuth(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("토큰이 없습니다.");
            }

            String token = authHeader.substring(7);
            if (!adminService.validateToken(token)) {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }

            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "message", "인증 성공"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }
}
