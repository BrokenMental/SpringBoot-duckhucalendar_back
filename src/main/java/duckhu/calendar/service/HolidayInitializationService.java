package duckhu.calendar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * 애플리케이션 시작 시 공휴일 데이터 초기화 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayInitializationService {

    private final HolidayService holidayService;

    /**
     * 애플리케이션 시작 완료 후 공휴일 데이터 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeHolidaysOnStartup() {
        log.info("🚀 애플리케이션 시작 완료 - 공휴일 데이터 초기화 시작");

        try {
            int currentYear = Year.now().getValue();
            int nextYear = currentYear + 1;

            // 현재 연도와 다음 연도 공휴일 초기화
            initializeYearHolidays(currentYear);
            initializeYearHolidays(nextYear);

            // 작년 데이터도 확인 (혹시 부족할 수 있으므로)
            initializeYearHolidays(currentYear - 1);

            log.info("✅ 공휴일 데이터 초기화 완료");

        } catch (Exception e) {
            log.error("❌ 공휴일 데이터 초기화 실패: {}", e.getMessage());
        }
    }

    /**
     * 특정 연도 공휴일 초기화
     * @param year 연도
     */
    private void initializeYearHolidays(int year) {
        try {
            log.info("{}년 공휴일 데이터 확인 중...", year);

            // 현재 DB에 저장된 공휴일 수 확인
            var existingHolidays = holidayService.getHolidaysByYear(year, "KR");

            if (existingHolidays.size() >= 5) {
                log.info("{}년 공휴일 데이터가 이미 충분합니다. ({}/8개 이상)", year, existingHolidays.size());
                return;
            }

            log.info("{}년 공휴일 데이터가 부족합니다. ({}/8개) - 공공데이터 동기화 시작", year, existingHolidays.size());

            // 공공데이터 API에서 동기화
            holidayService.syncHolidaysFromPublicAPI(year, "KR");

            // 동기화 후 결과 확인
            var updatedHolidays = holidayService.getHolidaysByYear(year, "KR");
            log.info("✅ {}년 공휴일 동기화 완료: {}개", year, updatedHolidays.size());

        } catch (Exception e) {
            log.warn("⚠️ {}년 공휴일 초기화 실패 (기본 데이터 사용): {}", year, e.getMessage());

            try {
                // 공공 API 실패 시 기본 데이터 생성
                holidayService.createDefaultKoreanHolidays(year);
                log.info("📋 {}년 기본 공휴일 데이터 생성 완료", year);
            } catch (Exception fallbackError) {
                log.error("❌ {}년 기본 공휴일 데이터 생성도 실패: {}", year, fallbackError.getMessage());
            }
        }
    }
}
