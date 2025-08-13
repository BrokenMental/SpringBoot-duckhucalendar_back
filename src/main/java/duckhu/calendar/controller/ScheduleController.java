package duckhu.calendar.controller;

import duckhu.calendar.dto.ScheduleRequestDto;
import duckhu.calendar.dto.ScheduleResponseDto;
import duckhu.calendar.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 향상된 일정 REST API 컨트롤러
 * 이미지, 링크, 추천 기능 등을 포함한 향상된 일정 관리 API
 */
@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Autowired
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 모든 일정 조회
     * GET /api/schedules
     */
    @GetMapping
    public ResponseEntity<?> getAllSchedules(
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sortBy) {
        try {
            List<ScheduleResponseDto> schedules;

            if (featured != null && featured) {
                schedules = scheduleService.getFeaturedSchedules();
            } else {
                schedules = scheduleService.getAllSchedules(sortBy);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("featuredCount", schedules.stream()
                    .mapToInt(s -> s.getIsFeatured() ? 1 : 0).sum());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("일정 목록 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 특정 ID의 일정 조회 (조회수 증가)
     * GET /api/schedules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        try {
            ScheduleResponseDto schedule = scheduleService.getScheduleByIdWithViewCount(id);
            return ResponseEntity.ok(schedule);
        } catch (RuntimeException e) {
            return createErrorResponse("일정 조회에 실패했습니다.", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("일정 조회 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 새 일정 생성 (관리자 전용)
     * POST /api/schedules
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSchedule(@Valid @RequestBody ScheduleRequestDto requestDto,
                                            BindingResult bindingResult) {
        try {
            // 유효성 검사 오류 확인
            if (bindingResult.hasErrors()) {
                return createValidationErrorResponse(bindingResult);
            }

            // 추가 비즈니스 로직 검증
            if (!requestDto.isValid()) {
                return createErrorResponse("입력 데이터가 올바르지 않습니다.",
                        "날짜, 시간, 이미지, 링크 정보를 다시 확인해주세요.", HttpStatus.BAD_REQUEST);
            }

            ScheduleResponseDto createdSchedule = scheduleService.createSchedule(requestDto);

            Map<String, Object> response = new HashMap<>();
            response.put("schedule", createdSchedule);
            response.put("message", "일정이 성공적으로 생성되었습니다.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return createErrorResponse("일정 생성에 실패했습니다.", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("일정 생성 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 일정 수정 (관리자 전용)
     * PUT /api/schedules/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
                                            @Valid @RequestBody ScheduleRequestDto requestDto,
                                            BindingResult bindingResult) {
        try {
            // 유효성 검사 오류 확인
            if (bindingResult.hasErrors()) {
                return createValidationErrorResponse(bindingResult);
            }

            // 추가 비즈니스 로직 검증
            if (!requestDto.isValid()) {
                return createErrorResponse("입력 데이터가 올바르지 않습니다.",
                        "날짜, 시간, 이미지, 링크 정보를 다시 확인해주세요.", HttpStatus.BAD_REQUEST);
            }

            ScheduleResponseDto updatedSchedule = scheduleService.updateSchedule(id, requestDto);

            Map<String, Object> response = new HashMap<>();
            response.put("schedule", updatedSchedule);
            response.put("message", "일정이 성공적으로 수정되었습니다.");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return createErrorResponse("일정 수정에 실패했습니다.", e.getMessage(), HttpStatus.NOT_FOUND);
            }
            return createErrorResponse("일정 수정에 실패했습니다.", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("일정 수정 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 일정 삭제 (관리자 전용)
     * DELETE /api/schedules/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        try {
            scheduleService.deleteSchedule(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "일정이 성공적으로 삭제되었습니다.");
            response.put("deletedId", id);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse("일정 삭제에 실패했습니다.", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("일정 삭제 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 특정 날짜의 일정 조회
     * GET /api/schedules/date/{date}
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getSchedulesByDate(@PathVariable String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            List<ScheduleResponseDto> schedules = scheduleService.getSchedulesByDate(targetDate);

            Map<String, Object> response = new HashMap<>();
            response.put("date", date);
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("featuredCount", schedules.stream()
                    .mapToInt(s -> s.getIsFeatured() ? 1 : 0).sum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("날짜별 일정 조회에 실패했습니다.",
                    "올바른 날짜 형식(YYYY-MM-DD)으로 입력해주세요.");
        }
    }

    /**
     * 특정 날짜 범위의 일정 조회
     * GET /api/schedules/range?start=2024-01-01&end=2024-01-31
     */
    @GetMapping("/range")
    public ResponseEntity<?> getSchedulesByDateRange(@RequestParam String start,
                                                     @RequestParam String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);

            if (startDate.isAfter(endDate)) {
                return createErrorResponse("날짜 범위 오류", "시작 날짜가 종료 날짜보다 늦습니다.", HttpStatus.BAD_REQUEST);
            }

            List<ScheduleResponseDto> schedules = scheduleService.getSchedulesByDateRange(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("featuredCount", schedules.stream()
                    .mapToInt(s -> s.getIsFeatured() ? 1 : 0).sum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("날짜 범위 일정 조회에 실패했습니다.",
                    "올바른 날짜 형식(YYYY-MM-DD)으로 입력해주세요.");
        }
    }

    /**
     * 특정 월의 일정 조회
     * GET /api/schedules/month/{year}/{month}
     */

    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<?> getSchedulesByMonth(@PathVariable int year,
                                                 @PathVariable int month) {
        try {
            if (month < 1 || month > 12) {
                return createErrorResponse("월 범위 오류", "월은 1-12 사이의 값이어야 합니다.", HttpStatus.BAD_REQUEST);
            }

            List<ScheduleResponseDto> schedules = scheduleService.getSchedulesByMonth(year, month);

            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("month", month);
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("featuredCount", schedules.stream()
                    .mapToInt(s -> s.getIsFeatured() ? 1 : 0).sum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("월별 일정 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 일정 검색
     * GET /api/schedules/search?title=검색어&category=FESTIVAL
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSchedules(@RequestParam(required = false) String title,
                                             @RequestParam(required = false) String category) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.searchSchedules(title, category);

            Map<String, Object> response = new HashMap<>();
            response.put("query", Map.of(
                    "title", title != null ? title : "",
                    "category", category != null ? category : ""
            ));
            response.put("schedules", schedules);
            response.put("count", schedules.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("일정 검색에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 오늘의 일정 조회
     * GET /api/schedules/today
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodaySchedules() {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getTodaySchedules();

            Map<String, Object> response = new HashMap<>();
            response.put("date", LocalDate.now().toString());
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("featuredCount", schedules.stream()
                    .mapToInt(s -> s.getIsFeatured() ? 1 : 0).sum());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("오늘 일정 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 다가오는 일정 조회
     * GET /api/schedules/upcoming?days=7
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingSchedules(@RequestParam(defaultValue = "7") int days) {
        try {
            if (days < 1 || days > 365) {
                return createErrorResponse("일수 범위 오류", "일수는 1-365 사이의 값이어야 합니다.", HttpStatus.BAD_REQUEST);
            }

            List<ScheduleResponseDto> schedules = scheduleService.getUpcomingSchedules(days);

            Map<String, Object> response = new HashMap<>();
            response.put("days", days);
            response.put("startDate", LocalDate.now().toString());
            response.put("endDate", LocalDate.now().plusDays(days).toString());
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("featuredCount", schedules.stream()
                    .mapToInt(s -> s.getIsFeatured() ? 1 : 0).sum());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("다가오는 일정 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 추천 이벤트 토글 (관리자 전용)
     * PATCH /api/schedules/{id}/featured
     */
    @PatchMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleFeatured(@PathVariable Long id,
                                            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isFeatured = request.get("isFeatured");
            if (isFeatured == null) {
                return createErrorResponse("잘못된 요청", "isFeatured 값이 필요합니다.", HttpStatus.BAD_REQUEST);
            }

            ScheduleResponseDto updatedSchedule = scheduleService.toggleFeatured(id, isFeatured);

            Map<String, Object> response = new HashMap<>();
            response.put("schedule", updatedSchedule);
            response.put("message", isFeatured ? "추천 이벤트로 설정되었습니다." : "추천 이벤트에서 해제되었습니다.");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse("추천 설정 변경에 실패했습니다.", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("추천 설정 변경 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 조회수 증가
     * POST /api/schedules/{id}/view
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long id) {
        try {
            scheduleService.incrementViewCount(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "조회수가 증가되었습니다.");
            response.put("scheduleId", id);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return createErrorResponse("조회수 증가에 실패했습니다.", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("조회수 증가 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 추천 이벤트 목록 조회
     * GET /api/schedules/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedSchedules(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getFeaturedSchedules(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("limit", limit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("추천 이벤트 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 인기 이벤트 목록 조회 (조회수 기준)
     * GET /api/schedules/popular
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularSchedules(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getPopularSchedules(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("limit", limit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("인기 이벤트 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 최근 추가된 이벤트 조회
     * GET /api/schedules/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentSchedules(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getRecentSchedules(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("schedules", schedules);
            response.put("count", schedules.size());
            response.put("limit", limit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("최근 이벤트 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 일정 통계 조회
     * GET /api/schedules/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getScheduleStats() {
        try {
            Map<String, Object> stats = scheduleService.getScheduleStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return createErrorResponse("일정 통계 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * API 상태 확인 (헬스체크)
     * GET /api/schedules/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Enhanced Calendar API is running");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("version", "2.0.0");
        response.put("features", List.of(
                "이미지 업로드 지원",
                "링크 관리",
                "추천 이벤트",
                "조회수 추적",
                "향상된 검색"
        ));

        return ResponseEntity.ok(response);
    }

    // === 유틸리티 메서드들 ===

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String message, String details) {
        return createErrorResponse(message, details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 에러 응답 생성 (HTTP 상태 코드 지정)
     */
    private ResponseEntity<?> createErrorResponse(String message, String details, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("details", details);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", status.value());

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 유효성 검사 오류 응답 생성
     */
    private ResponseEntity<?> createValidationErrorResponse(BindingResult bindingResult) {
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        bindingResult.getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        errorResponse.put("error", true);
        errorResponse.put("message", "입력 데이터가 유효하지 않습니다.");
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
