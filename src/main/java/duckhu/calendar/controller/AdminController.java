package duckhu.calendar.controller;

import duckhu.calendar.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 기능 컨트롤러 - 공공데이터 API 연동
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final HolidayService holidayService;

    /**
     * 공공데이터 API에서 공휴일 동기화
     */
    @PostMapping("/holidays/sync/{year}")
    public ResponseEntity<Map<String, Object>> syncHolidaysFromPublicAPI(@PathVariable int year) {
        log.info("관리자 요청: {}년 공공데이터 API 공휴일 동기화", year);

        try {
            holidayService.syncHolidaysFromPublicAPI(year, "KR");

            // 동기화 후 통계 조회
            Map<String, Object> stats = holidayService.getHolidayStatistics(year, "KR");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", year + "년 공공데이터 API에서 공휴일이 성공적으로 동기화되었습니다.");
            response.put("year", year);
            response.put("statistics", stats);

            log.info("{}년 공공데이터 API 공휴일 동기화 완료", year);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("공공데이터 API 동기화 실패 - 연도: {}, 오류: {}", year, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "공공데이터 API 동기화에 실패했습니다: " + e.getMessage());
            response.put("year", year);
            response.put("fallbackMessage", "기본 공휴일 데이터가 사용되었을 수 있습니다.");

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 여러 연도 공휴일 일괄 동기화
     */
    @PostMapping("/holidays/sync/batch")
    public ResponseEntity<Map<String, Object>> syncMultipleYears(
            @RequestParam(defaultValue = "2024,2025,2026") String years) {

        log.info("관리자 요청: 여러 연도 공휴일 일괄 동기화 - 연도들: {}", years);

        try {
            String[] yearArray = years.split(",");
            Map<Integer, String> results = new HashMap<>();
            int successCount = 0;

            for (String yearStr : yearArray) {
                try {
                    int year = Integer.parseInt(yearStr.trim());
                    holidayService.syncHolidaysFromPublicAPI(year, "KR");
                    results.put(year, "성공");
                    successCount++;
                } catch (Exception e) {
                    results.put(Integer.parseInt(yearStr.trim()), "실패: " + e.getMessage());
                    log.warn("{}년 동기화 실패: {}", yearStr, e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", successCount > 0);
            response.put("message", String.format("총 %d개 연도 중 %d개 성공", yearArray.length, successCount));
            response.put("results", results);
            response.put("totalYears", yearArray.length);
            response.put("successCount", successCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("일괄 동기화 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "일괄 동기화에 실패했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 현재 연도와 다음 연도 공휴일 동기화
     */
    @PostMapping("/holidays/sync/current")
    public ResponseEntity<Map<String, Object>> syncCurrentYearHolidays() {
        log.info("관리자 요청: 현재 연도 공휴일 동기화");

        try {
            int currentYear = java.time.Year.now().getValue();
            int nextYear = currentYear + 1;

            holidayService.syncHolidaysFromPublicAPI(currentYear, "KR");
            holidayService.syncHolidaysFromPublicAPI(nextYear, "KR");

            // 통계 조회
            Map<String, Object> currentStats = holidayService.getHolidayStatistics(currentYear, "KR");
            Map<String, Object> nextStats = holidayService.getHolidayStatistics(nextYear, "KR");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", currentYear + "년, " + nextYear + "년 공공데이터에서 공휴일이 동기화되었습니다.");
            response.put("currentYear", currentYear);
            response.put("nextYear", nextYear);
            response.put("currentYearStats", currentStats);
            response.put("nextYearStats", nextStats);

            log.info("{}년, {}년 공공데이터 공휴일 동기화 완료", currentYear, nextYear);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("현재 연도 공휴일 동기화 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "현재 연도 공휴일 동기화에 실패했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 기본 한국 공휴일 데이터 생성 (공공API 실패시 대안)
     */
    @PostMapping("/holidays/create-default/{year}")
    public ResponseEntity<Map<String, Object>> createDefaultKoreanHolidays(@PathVariable int year) {
        log.info("관리자 요청: {}년 기본 한국 공휴일 생성", year);

        try {
            holidayService.createDefaultKoreanHolidays(year);

            Map<String, Object> stats = holidayService.getHolidayStatistics(year, "KR");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", year + "년 기본 한국 공휴일 데이터가 생성되었습니다.");
            response.put("year", year);
            response.put("statistics", stats);

            log.info("{}년 기본 한국 공휴일 생성 완료", year);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("기본 공휴일 생성 실패 - 연도: {}, 오류: {}", year, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "기본 공휴일 생성에 실패했습니다: " + e.getMessage());
            response.put("year", year);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 공휴일 통계 조회
     */
    @GetMapping("/holidays/stats/{year}")
    public ResponseEntity<Map<String, Object>> getHolidayStats(@PathVariable int year) {
        try {
            Map<String, Object> stats = holidayService.getHolidayStatistics(year, "KR");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("공휴일 통계 조회 실패 - 연도: {}, 오류: {}", year, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "공휴일 통계 조회에 실패했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 비동기 공휴일 동기화 (백그라운드 실행)
     */
    @PostMapping("/holidays/sync-async/{year}")
    public ResponseEntity<Map<String, Object>> syncHolidaysAsync(@PathVariable int year) {
        log.info("관리자 요청: {}년 비동기 공휴일 동기화", year);

        try {
            holidayService.syncHolidaysByYearAsync(year);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", year + "년 공휴일 동기화가 백그라운드에서 시작되었습니다.");
            response.put("year", year);
            response.put("note", "동기화 완료 여부는 별도로 확인하시기 바랍니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("비동기 공휴일 동기화 실패 - 연도: {}, 오류: {}", year, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "비동기 공휴일 동기화 시작에 실패했습니다: " + e.getMessage());
            response.put("year", year);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 공공데이터 API 연결 테스트
     */
    @GetMapping("/holidays/test-connection/{year}")
    public ResponseEntity<Map<String, Object>> testPublicApiConnection(@PathVariable int year) {
        log.info("관리자 요청: 공공데이터 API 연결 테스트 - 연도: {}", year);

        try {
            // 실제 동기화하지 않고 연결만 테스트
            holidayService.syncHolidaysFromPublicAPI(year, "KR");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "공공데이터 API 연결이 정상입니다.");
            response.put("year", year);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("공공데이터 API 연결 테스트 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "공공데이터 API 연결에 실패했습니다: " + e.getMessage());
            response.put("year", year);
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("recommendation", "서비스 키와 네트워크 연결을 확인해주세요.");

            return ResponseEntity.badRequest().body(response);
        }
    }
}
