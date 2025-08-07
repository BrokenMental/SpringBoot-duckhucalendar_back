package duckhu.calendar.controller;

import duckhu.calendar.dto.ScheduleRequestDto;
import duckhu.calendar.dto.ScheduleResponseDto;
import duckhu.calendar.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 일정 REST API 컨트롤러
 * Vue 프론트엔드와 통신하기 위한 REST API 엔드포인트 제공
 */
@RestController
@RequestMapping("/schedules")
@CrossOrigin(origins = "http://localhost:5173") // Vue 개발 서버 허용
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
    public ResponseEntity<?> getAllSchedules() {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getAllSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            return createErrorResponse("일정 목록 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 특정 ID의 일정 조회
     * GET /api/schedules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        try {
            ScheduleResponseDto schedule = scheduleService.getScheduleById(id);
            return ResponseEntity.ok(schedule);
        } catch (RuntimeException e) {
            return createErrorResponse("일정 조회에 실패했습니다.", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("일정 조회 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 새 일정 생성
     * POST /api/schedules
     */
    @PostMapping
    public ResponseEntity<?> createSchedule(@Valid @RequestBody ScheduleRequestDto requestDto,
                                            BindingResult bindingResult) {
        try {
            // 유효성 검사 오류 확인
            if (bindingResult.hasErrors()) {
                return createValidationErrorResponse(bindingResult);
            }

            ScheduleResponseDto createdSchedule = scheduleService.createSchedule(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSchedule);

        } catch (RuntimeException e) {
            return createErrorResponse("일정 생성에 실패했습니다.", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("일정 생성 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 일정 수정
     * PUT /api/schedules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
                                            @Valid @RequestBody ScheduleRequestDto requestDto,
                                            BindingResult bindingResult) {
        try {
            // 유효성 검사 오류 확인
            if (bindingResult.hasErrors()) {
                return createValidationErrorResponse(bindingResult);
            }

            ScheduleResponseDto updatedSchedule = scheduleService.updateSchedule(id, requestDto);
            return ResponseEntity.ok(updatedSchedule);

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
     * 일정 삭제
     * DELETE /api/schedules/{id}
     */
    @DeleteMapping("/{id}")
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
     * @param date YYYY-MM-DD 형식
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getSchedulesByDate(@PathVariable String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            List<ScheduleResponseDto> schedules = scheduleService.getSchedulesByDate(targetDate);
            return ResponseEntity.ok(schedules);

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
            return ResponseEntity.ok(schedules);

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
            return ResponseEntity.ok(schedules);

        } catch (Exception e) {
            return createErrorResponse("월별 일정 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 제목으로 일정 검색
     * GET /api/schedules/search?title=검색어
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSchedules(@RequestParam(required = false) String title) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.searchSchedulesByTitle(title);
            return ResponseEntity.ok(schedules);
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
            response.put("count", schedules.size());
            response.put("schedules", schedules);

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
            response.put("count", schedules.size());
            response.put("schedules", schedules);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("다가오는 일정 조회에 실패했습니다.", e.getMessage());
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
        response.put("message", "Calendar API is running");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("version", "1.0.0");

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
