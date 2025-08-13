package duckhu.calendar.controller;

import duckhu.calendar.dto.HolidayDTO;
import duckhu.calendar.entity.Holiday;
import duckhu.calendar.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공휴일/국경일 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 날짜 범위별 공휴일/국경일 조회
     * Vue.js에서 보내는 파라미터명에 맞춰 수정
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getHolidaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "KR") String countryCode) {

        log.info("공휴일 조회 요청 - 시작일: {}, 종료일: {}, 국가: {}", startDate, endDate, countryCode);

        try {
            List<HolidayDTO> holidays = holidayService.getHolidaysByDateRange(startDate, endDate, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("holidays", holidays);
            response.put("count", holidays.size());

            log.info("조회된 공휴일 수: {}", holidays.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("날짜 범위별 공휴일 조회 실패: {}", e.getMessage());

            // 에러 시 기본 공휴일 반환
            List<HolidayDTO> defaultHolidays = getDefaultHolidaysForDateRange(startDate, endDate, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("holidays", defaultHolidays);
            response.put("count", defaultHolidays.size());
            response.put("message", "기본 공휴일 데이터를 제공합니다.");
            response.put("error", e.getMessage());

            return ResponseEntity.ok(response); // 500 대신 200으로 반환
        }
    }

    /**
     * 날짜 범위에 대한 기본 공휴일 생성
     */
    private List<HolidayDTO> getDefaultHolidaysForDateRange(LocalDate startDate, LocalDate endDate, String countryCode) {
        List<HolidayDTO> holidays = new ArrayList<>();

        if (!"KR".equals(countryCode)) {
            return holidays; // 한국이 아니면 빈 리스트 반환
        }

        // 해당 범위의 연도들 추출
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            List<HolidayDTO> yearHolidays = getDefaultHolidaysForError(year, countryCode);

            // 날짜 범위 필터링
            for (HolidayDTO holiday : yearHolidays) {
                LocalDate holidayDate = holiday.getHolidayDate();
                if (!holidayDate.isBefore(startDate) && !holidayDate.isAfter(endDate)) {
                    holidays.add(holiday);
                }
            }
        }

        return holidays;
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
            response.put("count", holidays != null ? holidays.size() : 0);

            log.info("{}년 공휴일 {}개 조회 완료", year, holidays != null ? holidays.size() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("연도별 공휴일 조회 실패: {}", e.getMessage());

            // 실패 시 기본 공휴일 반환
            List<HolidayDTO> defaultHolidays = getDefaultHolidaysForError(year, countryCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("year", year);
            response.put("countryCode", countryCode);
            response.put("holidays", defaultHolidays);
            response.put("count", defaultHolidays.size());
            response.put("message", "기본 공휴일 데이터를 제공합니다.");

            return ResponseEntity.ok(response); // 200 OK로 반환하여 프론트엔드에서 처리할 수 있도록
        }
    }

    /**
     * 에러 발생 시 기본 공휴일 반환
     */
    private List<HolidayDTO> getDefaultHolidaysForError(int year, String countryCode) {
        List<HolidayDTO> holidays = new ArrayList<>();

        if ("KR".equals(countryCode)) {
            addDefaultHoliday(holidays, year, 1, 1, "신정");
            addDefaultHoliday(holidays, year, 3, 1, "삼일절");
            addDefaultHoliday(holidays, year, 5, 5, "어린이날");
            addDefaultHoliday(holidays, year, 6, 6, "현충일");
            addDefaultHoliday(holidays, year, 8, 15, "광복절");
            addDefaultHoliday(holidays, year, 10, 3, "개천절");
            addDefaultHoliday(holidays, year, 10, 9, "한글날");
            addDefaultHoliday(holidays, year, 12, 25, "크리스마스");
        }

        return holidays;
    }

    /**
     * 기본 공휴일 생성 헬퍼
     */
    private void addDefaultHoliday(List<HolidayDTO> holidays, int year, int month, int day, String name) {
        HolidayDTO holiday = new HolidayDTO();
        holiday.setId(-1L);
        holiday.setName(name);
        holiday.setHolidayDate(LocalDate.of(year, month, day));
        holiday.setCountryCode("KR");
        holiday.setHolidayType(Holiday.HolidayType.PUBLIC);
        holiday.setDescription(name);
        holiday.setIsRecurring(true);
        holiday.setColor("#FF6B6B");
        holidays.add(holiday);
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
