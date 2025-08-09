package duckhu.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import duckhu.calendar.entity.Holiday;
import duckhu.calendar.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공휴일 관리 서비스
 */
@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public HolidayService(HolidayRepository holidayRepository, RestTemplate restTemplate) {
        this.holidayRepository = holidayRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 특정 날짜의 공휴일 조회
     * @param date 조회할 날짜
     * @return 공휴일 목록
     */
    public List<Holiday> getHolidaysByDate(LocalDate date) {
        return holidayRepository.findByHolidayDateOrderByHolidayTypeAsc(date);
    }

    /**
     * 특정 연도의 모든 공휴일 조회
     * @param year 연도
     * @return 공휴일 목록
     */
    public List<Holiday> getHolidaysByYear(int year) {
        List<Holiday> holidays = holidayRepository.findByYear(year);

        // 공휴일이 없으면 외부 API에서 가져오기 시도
        if (holidays.isEmpty()) {
            System.out.println(year + "년 공휴일이 없어서 API 동기화 시작...");

            try {
                // 먼저 외부 API 시도
                syncHolidaysFromAPISync(year);
                holidays = holidayRepository.findByYear(year);

                // API에서 가져온 것이 없으면 기본 공휴일 추가
                if (holidays.isEmpty()) {
                    System.out.println("API에서 공휴일을 가져오지 못해 기본 공휴일 추가...");
                    addDefaultKoreanHolidays(year);
                    holidays = holidayRepository.findByYear(year);
                }

            } catch (Exception e) {
                System.err.println("API 동기화 실패, 기본 공휴일 추가: " + e.getMessage());
                // API 실패 시 기본 공휴일 추가
                addDefaultKoreanHolidays(year);
                holidays = holidayRepository.findByYear(year);
            }
        }

        return holidays;
    }

    /**
     * 외부 API에서 공휴일 정보 가져오기 (동기 방식)
     * @param year 조회할 연도
     */
    private void syncHolidaysFromAPISync(int year) {
        try {
            // Nager.Date API 사용 (무료)
            String url = "https://date.nager.at/api/v3/publicholidays/" + year + "/KR";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode holidaysJson = objectMapper.readTree(response);

            int addedCount = 0;
            for (JsonNode holidayNode : holidaysJson) {
                String name = holidayNode.get("localName").asText();
                LocalDate date = LocalDate.parse(holidayNode.get("date").asText());

                // 중복 체크
                if (!holidayRepository.existsByNameAndHolidayDate(name, date)) {
                    Holiday holiday = new Holiday();
                    holiday.setName(name);
                    holiday.setHolidayDate(date);
                    holiday.setCountryCode("KR");
                    holiday.setHolidayType(Holiday.HolidayType.PUBLIC);
                    holiday.setDescription("API에서 자동 동기화된 공휴일");
                    holiday.setColor("#FF6B6B");

                    holidayRepository.save(holiday);
                    addedCount++;
                }
            }

            System.out.println(year + "년 공휴일 " + addedCount + "개가 추가되었습니다.");

        } catch (Exception e) {
            System.err.println("공휴일 API 동기화 실패: " + e.getMessage());
            throw new RuntimeException("공휴일 API 동기화 실패", e);
        }
    }

    /**
     * 특정 월의 공휴일 조회
     * @param year 연도
     * @param month 월
     * @return 공휴일 목록
     */
    public List<Holiday> getHolidaysByMonth(int year, int month) {
        return holidayRepository.findByYearAndMonth(year, month);
    }

    /**
     * 날짜 범위별 공휴일 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 공휴일 목록
     */
    public List<Holiday> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate);
    }

    /**
     * 오늘의 공휴일 조회
     * @return 오늘의 공휴일 목록
     */
    public List<Holiday> getTodayHolidays() {
        return holidayRepository.findTodayHolidays();
    }

    /**
     * 공휴일 추가
     * @param holiday 추가할 공휴일
     * @return 저장된 공휴일
     */
    @Transactional
    public Holiday addHoliday(Holiday holiday) {
        // 중복 체크
        if (holidayRepository.existsByNameAndHolidayDate(holiday.getName(), holiday.getHolidayDate())) {
            throw new RuntimeException("이미 등록된 공휴일입니다: " + holiday.getName() + " (" + holiday.getHolidayDate() + ")");
        }
        return holidayRepository.save(holiday);
    }

    /**
     * 공휴일 수정
     * @param id 공휴일 ID
     * @param updatedHoliday 수정할 공휴일 정보
     * @return 수정된 공휴일
     */
    @Transactional
    public Holiday updateHoliday(Long id, Holiday updatedHoliday) {
        Holiday existingHoliday = holidayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공휴일을 찾을 수 없습니다. ID: " + id));

        existingHoliday.setName(updatedHoliday.getName());
        existingHoliday.setHolidayDate(updatedHoliday.getHolidayDate());
        existingHoliday.setHolidayType(updatedHoliday.getHolidayType());
        existingHoliday.setDescription(updatedHoliday.getDescription());
        existingHoliday.setColor(updatedHoliday.getColor());
        existingHoliday.setIsRecurring(updatedHoliday.getIsRecurring());

        return holidayRepository.save(existingHoliday);
    }

    /**
     * 공휴일 삭제
     * @param id 공휴일 ID
     */
    @Transactional
    public void deleteHoliday(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공휴일을 찾을 수 없습니다. ID: " + id));
        holidayRepository.delete(holiday);
    }

    /**
     * 외부 API에서 공휴일 정보 가져오기 (한국)
     * @param year 조회할 연도
     * @return 성공 여부
     */
    @Async
    @Transactional
    public void syncHolidaysFromAPI(int year) {
        try {
            // Nager.Date API 사용 (무료)
            String url = "https://date.nager.at/api/v3/publicholidays/" + year + "/KR";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode holidaysJson = objectMapper.readTree(response);

            int addedCount = 0;
            for (JsonNode holidayNode : holidaysJson) {
                String name = holidayNode.get("localName").asText();
                LocalDate date = LocalDate.parse(holidayNode.get("date").asText());

                // 중복 체크
                if (!holidayRepository.existsByNameAndHolidayDate(name, date)) {
                    Holiday holiday = new Holiday();
                    holiday.setName(name);
                    holiday.setHolidayDate(date);
                    holiday.setCountryCode("KR");
                    holiday.setHolidayType(Holiday.HolidayType.PUBLIC);
                    holiday.setDescription("API에서 자동 동기화된 공휴일");

                    holidayRepository.save(holiday);
                    addedCount++;
                }
            }

            System.out.println(year + "년 공휴일 " + addedCount + "개가 추가되었습니다.");

        } catch (Exception e) {
            System.err.println("공휴일 API 동기화 실패: " + e.getMessage());
            // API 실패 시 기본 한국 공휴일 추가
            addDefaultKoreanHolidays(year);
        }
    }

    /**
     * 기본 한국 공휴일 추가 (API 실패 시 폴백)
     * @param year 연도
     */
    @Transactional
    public void addDefaultKoreanHolidays(int year) {
        Map<String, String> defaultHolidays = new HashMap<>();
        defaultHolidays.put("01-01", "신정");
        defaultHolidays.put("03-01", "삼일절");
        defaultHolidays.put("05-05", "어린이날");
        defaultHolidays.put("06-06", "현충일");
        defaultHolidays.put("08-15", "광복절");
        defaultHolidays.put("10-03", "개천절");
        defaultHolidays.put("10-09", "한글날");
        defaultHolidays.put("12-25", "크리스마스");

        int addedCount = 0;
        for (Map.Entry<String, String> entry : defaultHolidays.entrySet()) {
            String dateStr = year + "-" + entry.getKey();
            LocalDate date = LocalDate.parse(dateStr);
            String name = entry.getValue();

            if (!holidayRepository.existsByNameAndHolidayDate(name, date)) {
                Holiday holiday = new Holiday();
                holiday.setName(name);
                holiday.setHolidayDate(date);
                holiday.setCountryCode("KR");
                holiday.setHolidayType(Holiday.HolidayType.PUBLIC);
                holiday.setDescription("기본 한국 공휴일");
                holiday.setIsRecurring(true);

                holidayRepository.save(holiday);
                addedCount++;
            }
        }

        System.out.println(year + "년 기본 공휴일 " + addedCount + "개가 추가되었습니다.");
    }

    /**
     * 특정 날짜의 공휴일 개수 조회
     * @param date 날짜
     * @return 공휴일 개수
     */
    public int getHolidayCountByDate(LocalDate date) {
        return getHolidaysByDate(date).size();
    }

    /**
     * 공휴일 통계 조회
     * @param year 연도
     * @return 통계 정보
     */
    public Map<String, Object> getHolidayStatistics(int year) {
        Map<String, Object> stats = new HashMap<>();

        long totalHolidays = holidayRepository.countByYear(year);
        List<Holiday> holidays = holidayRepository.findByYear(year);

        stats.put("totalHolidays", totalHolidays);
        stats.put("year", year);

        // 타입별 통계
        Map<String, Long> typeStats = new HashMap<>();
        for (Holiday.HolidayType type : Holiday.HolidayType.values()) {
            long count = holidays.stream()
                    .filter(h -> h.getHolidayType() == type)
                    .count();
            typeStats.put(type.name(), count);
        }
        stats.put("byType", typeStats);

        return stats;
    }
}
