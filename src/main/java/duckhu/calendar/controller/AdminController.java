package duckhu.calendar.controller;

import duckhu.calendar.config.JwtUtil;
import duckhu.calendar.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 인증 및 관리 기능 컨트롤러
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AdminController(AdminService adminService, JwtUtil jwtUtil) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 관리자 임시 비밀번호 요청
     * POST /api/admin/request-temp-password
     */
    @PostMapping("/request-temp-password")
    public ResponseEntity<?> requestTempPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return createErrorResponse("이메일을 입력해주세요.");
            }

            // 관리자 이메일 확인 및 임시 비밀번호 발송
            adminService.sendTempPassword(email);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "임시 비밀번호가 이메일로 전송되었습니다.");
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * 임시 비밀번호로 로그인
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginWithTempPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String tempPassword = request.get("tempPassword");

            if (email == null || tempPassword == null) {
                return createErrorResponse("이메일과 임시 비밀번호를 모두 입력해주세요.");
            }

            // 임시 비밀번호 검증
            boolean isValid = adminService.validateTempPassword(email, tempPassword);
            if (!isValid) {
                return createErrorResponse("올바르지 않은 임시 비밀번호입니다.");
            }

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(email);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인 성공");
            response.put("token", token);
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * 관리자 인증 상태 확인
     * GET /api/admin/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null || !jwtUtil.validateToken(token)) {
                return createErrorResponse("유효하지 않은 토큰입니다.");
            }

            String email = jwtUtil.getEmailFromToken(token);
            if (!adminService.isValidAdmin(email)) {
                return createErrorResponse("관리자 권한이 없습니다.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("role", "ADMIN");
            response.put("authenticated", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("인증 확인 중 오류가 발생했습니다.");
        }
    }

    /**
     * 로그아웃
     * POST /api/admin/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT는 상태가 없으므로 클라이언트에서 토큰만 제거하면 됨
        Map<String, Object> response = new HashMap<>();
        response.put("message", "로그아웃 되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
