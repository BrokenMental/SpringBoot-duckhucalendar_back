package duckhu.calendar.controller;

import duckhu.calendar.dto.HolidayDTO;
import duckhu.calendar.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공휴일/국경일 관리 컨트롤러
 */
@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 날짜 범위별 공휴일/국경일 조회
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getHolidaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "KR") String countryCode) {

        log.info("공휴일 조회 요청 - 시작일: {}, 종료일: {}, 국가: {}", start, end, countryCode);

        try {
            List<HolidayDTO> holidays = holidayService.getHolidaysByDateRange(start, end, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("startDate", start.toString());
            response.put("endDate", end.toString());
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            log.info("조회된 공휴일 수: {}", holidays.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("날짜 범위별 공휴일 조회 실패: {}", e.getMessage());
            return createErrorResponse("날짜 범위별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 특정 날짜의 공휴일/국경일 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<Map<String, Object>> getHolidaysByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "KR") String countryCode) {

        log.info("특정 날짜 공휴일 조회 - 날짜: {}, 국가: {}", date, countryCode);

        try {
            List<HolidayDTO> holidays = holidayService.getHolidaysByDate(date, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", date.toString());
            response.put("holidays", holidays);
            response.put("count", holidays.size());
            response.put("isHoliday", !holidays.isEmpty());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("특정 날짜 공휴일 조회 실패: {}", e.getMessage());
            return createErrorResponse("특정 날짜 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 연도별 공휴일/국경일 조회
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<Map<String, Object>> getHolidaysByYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "KR") String countryCode) {

        log.info("연도별 공휴일 조회 - 연도: {}, 국가: {}", year, countryCode);

        try {
            List<HolidayDTO> holidays = holidayService.getHolidaysByYear(year, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("year", year);
            response.put("countryCode", countryCode);
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("연도별 공휴일 조회 실패: {}", e.getMessage());
            return createErrorResponse("연도별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 월별 공휴일/국경일 조회
     */
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getHolidaysByMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "KR") String countryCode) {

        try {
            if (month < 1 || month > 12) {
                return createErrorResponse("올바르지 않은 월입니다.", "월은 1-12 사이의 값이어야 합니다.");
            }

            // 해당 월의 시작일과 끝일 계산
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            List<HolidayDTO> holidays = holidayService.getHolidaysByDateRange(startDate, endDate, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("year", year);
            response.put("month", month);
            response.put("countryCode", countryCode);
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("월별 공휴일 조회 실패: {}", e.getMessage());
            return createErrorResponse("월별 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 오늘의 공휴일 조회
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayHolidays(
            @RequestParam(defaultValue = "KR") String countryCode) {

        try {
            LocalDate today = LocalDate.now();
            List<HolidayDTO> holidays = holidayService.getHolidaysByDate(today, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", today.toString());
            response.put("holidays", holidays);
            response.put("count", holidays.size());
            response.put("isHoliday", !holidays.isEmpty());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("오늘 공휴일 조회 실패: {}", e.getMessage());
            return createErrorResponse("오늘 공휴일 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일/국경일 등록
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createHoliday(@Valid @RequestBody HolidayDTO holidayDTO) {
        log.info("공휴일 등록 요청: {}", holidayDTO.getName());

        try {
            HolidayDTO createdHoliday = holidayService.createHoliday(holidayDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "공휴일이 성공적으로 등록되었습니다.");
            response.put("holiday", createdHoliday);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse("공휴일 등록 실패", e.getMessage());
        } catch (Exception e) {
            log.error("공휴일 등록 중 오류 발생: {}", e.getMessage());
            return createErrorResponse("공휴일 등록 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일/국경일 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayDTO holidayDTO) {

        log.info("공휴일 수정 요청 - ID: {}", id);

        try {
            HolidayDTO updatedHoliday = holidayService.updateHoliday(id, holidayDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "공휴일이 성공적으로 수정되었습니다.");
            response.put("holiday", updatedHoliday);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse("공휴일 수정 실패", e.getMessage());
        } catch (Exception e) {
            log.error("공휴일 수정 중 오류 발생: {}", e.getMessage());
            return createErrorResponse("공휴일 수정 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일/국경일 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHoliday(@PathVariable Long id) {
        log.info("공휴일 삭제 요청 - ID: {}", id);

        try {
            holidayService.deleteHoliday(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "공휴일이 성공적으로 삭제되었습니다.");
            response.put("deletedId", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse("공휴일 삭제 실패", e.getMessage());
        } catch (Exception e) {
            log.error("공휴일 삭제 중 오류 발생: {}", e.getMessage());
            return createErrorResponse("공휴일 삭제 중 오류가 발생했습니다.", e.getMessage());
        }
    }

    /**
     * 공휴일 통계 조회
     */
    @GetMapping("/stats/{year}")
    public ResponseEntity<Map<String, Object>> getHolidayStatistics(
            @PathVariable int year,
            @RequestParam(defaultValue = "KR") String countryCode) {

        try {
            Map<String, Object> stats = holidayService.getHolidayStatistics(year, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("공휴일 통계 조회 실패: {}", e.getMessage());
            return createErrorResponse("공휴일 통계 조회에 실패했습니다.", e.getMessage());
        }
    }

    /**
     * 통일된 에러 응답 생성
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, String details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", true);
        response.put("message", message);
        response.put("details", details);
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(response);
    }
}
