package duckhu.calendar.controller;

import duckhu.calendar.entity.Holiday;
import duckhu.calendar.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공휴일 관리 컨트롤러
 */
@RestController
@RequestMapping("/holidays")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class HolidayController {

    private final HolidayService holidayService;

    @Autowired
    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /**
     * 특정 날짜의 공휴일 조회
     * GET /api/holidays/date/{date}
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getHolidaysByDate(@PathVariable String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<Holiday> holidays = holidayService.getHolidaysByDate(localDate);

            Map<String, Object> response = new HashMap<>();
            response.put("date", date);
            response.put("holidays", holidays);
            response.put("count", holidays.size());
            response.put("isHoliday", !holidays.isEmpty());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("날짜별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 특정 연도의 모든 공휴일 조회
     * GET /api/holidays/year/{year}
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<?> getHolidaysByYear(@PathVariable int year) {
        try {
            List<Holiday> holidays = holidayService.getHolidaysByYear(year);

            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("연도별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 특정 월의 공휴일 조회
     * GET /api/holidays/month/{year}/{month}
     */
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<?> getHolidaysByMonth(@PathVariable int year, @PathVariable int month) {
        try {
            if (month < 1 || month > 12) {
                return createErrorResponse("올바르지 않은 월입니다.", "월은 1-12 사이의 값이어야 합니다.");
            }

            List<Holiday> holidays = holidayService.getHolidaysByMonth(year, month);

            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("month", month);
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("월별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 날짜 범위별 공휴일 조회
     * GET /api/holidays/range?start=2025-01-01&end=2025-12-31
     */
    @GetMapping("/range")
    public ResponseEntity<?> getHolidaysByDateRange(@RequestParam String start, @RequestParam String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);

            if (startDate.isAfter(endDate)) {
                return createErrorResponse("날짜 범위 오류", "시작 날짜가 종료 날짜보다 늦습니다.");
            }

            List<Holiday> holidays = holidayService.getHolidaysByDateRange(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("날짜 범위별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 오늘의 공휴일 조회
     * GET /api/holidays/today
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodayHolidays() {
        try {
            List<Holiday> holidays = holidayService.getTodayHolidays();

            Map<String, Object> response = new HashMap<>();
            response.put("date", LocalDate.now().toString());
            response.put("holidays", holidays);
            response.put("count", holidays.size());
            response.put("isHoliday", !holidays.isEmpty());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("오늘 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일 추가 (관리자 전용)
     * POST /api/holidays
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addHoliday(@RequestBody Holiday holiday) {
        try {
            Holiday savedHoliday = holidayService.addHoliday(holiday);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "공휴일이 성공적으로 추가되었습니다.");
            response.put("holiday", savedHoliday);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("공휴일 추가에 실패했습니다.", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("공휴일 추가 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일 수정 (관리자 전용)
     * PUT /api/holidays/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateHoliday(@PathVariable Long id, @RequestBody Holiday holiday) {
        try {
            Holiday updatedHoliday = holidayService.updateHoliday(id, holiday);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "공휴일이 성공적으로 수정되었습니다.");
            response.put("holiday", updatedHoliday);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("공휴일 수정에 실패했습니다.", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("공휴일 수정 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일 삭제 (관리자 전용)
     * DELETE /api/holidays/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        try {
            holidayService.deleteHoliday(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "공휴일이 성공적으로 삭제되었습니다.");
            response.put("deletedId", id);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("공휴일 삭제에 실패했습니다.", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("공휴일 삭제 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 외부 API에서 공휴일 동기화 (관리자 전용)
     * POST /api/holidays/sync/{year}
     */
    @PostMapping("/sync/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncHolidaysFromAPI(@PathVariable int year) {
        try {
            // 비동기로 실행
            holidayService.syncHolidaysFromAPI(year);

            Map<String, Object> response = new HashMap<>();
            response.put("message", year + "년 공휴일 동기화가 시작되었습니다.");
            response.put("year", year);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("공휴일 동기화에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일 통계 조회
     * GET /api/holidays/stats/{year}
     */
    @GetMapping("/stats/{year}")
    public ResponseEntity<?> getHolidayStatistics(@PathVariable int year) {
        try {
            Map<String, Object> stats = holidayService.getHolidayStatistics(year);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return createErrorResponse("공휴일 통계 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String message, String details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("details", details);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(errorResponse);
    }
}