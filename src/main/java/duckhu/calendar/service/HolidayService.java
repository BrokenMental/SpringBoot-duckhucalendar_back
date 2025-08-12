package duckhu.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import duckhu.calendar.config.PublicDataConfig;
import duckhu.calendar.dto.HolidayDTO;
import duckhu.calendar.entity.Holiday;
import duckhu.calendar.repository.HolidayRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 공휴일/국경일 서비스 - 공공데이터 API 연동
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class HolidayService {
    private static final int MIN_EXPECTED_HOLIDAYS = 8; // 최소 예상 공휴일 수

    private final HolidayRepository holidayRepository;
    private final RestTemplate publicDataRestTemplate;
    private final PublicDataConfig publicDataConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public HolidayService(HolidayRepository holidayRepository,
                          @Qualifier("publicDataRestTemplate") RestTemplate publicDataRestTemplate,
                          PublicDataConfig publicDataConfig,
                          ObjectMapper objectMapper) {
        this.holidayRepository = holidayRepository;
        this.publicDataRestTemplate = publicDataRestTemplate;
        this.publicDataConfig = publicDataConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * 날짜 범위별 공휴일/국경일 조회
     */
    public List<HolidayDTO> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate, String countryCode) {
        log.debug("공휴일 조회 - 범위: {} ~ {}, 국가: {}", startDate, endDate, countryCode);

        List<Holiday> holidays = holidayRepository.findByHolidayDateBetweenAndCountryCodeOrderByHolidayDateAscHolidayTypeAsc(
                startDate, endDate, countryCode);

        return holidays.stream()
                .map(HolidayDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 공휴일/국경일 조회
     */
    public List<HolidayDTO> getHolidaysByDate(LocalDate date, String countryCode) {
        log.debug("특정 날짜 공휴일 조회 - 날짜: {}, 국가: {}", date, countryCode);

        List<Holiday> holidays = holidayRepository.findByHolidayDateAndCountryCodeOrderByHolidayTypeAsc(date, countryCode);

        return holidays.stream()
                .map(HolidayDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 연도별 공휴일/국경일 조회 (공공데이터 API 연동)
     */
    public List<HolidayDTO> getHolidaysByYear(int year, String countryCode) {
        List<Holiday> holidays = holidayRepository.findByYearAndCountryCode(year, countryCode);

        // 공휴일이 부족할 경우 별도 트랜잭션에서 동기화
        if (holidays.size() < MIN_EXPECTED_HOLIDAYS) {
            log.info("{}년 공휴일 데이터가 부족하여 공공 API에서 동기화합니다.", year);
            // 별도 메서드로 분리하여 새로운 트랜잭션에서 실행
            try {
                syncHolidaysInNewTransaction(year, countryCode);
                // 동기화 후 다시 조회
                holidays = holidayRepository.findByYearAndCountryCode(year, countryCode);
            } catch (Exception e) {
                log.warn("공휴일 동기화 실패: {}", e.getMessage());
            }
        }

        return holidays.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Entity를 DTO로 변환하는 메서드 추가
     */
    private HolidayDTO convertToDTO(Holiday holiday) {
        return HolidayDTO.fromEntity(holiday);
    }

    /**
     * 새로운 트랜잭션에서 공휴일 동기화
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncHolidaysInNewTransaction(int year, String countryCode) {
        try {
            syncHolidaysFromPublicAPI(year, countryCode);
        } catch (Exception e) {
            log.warn("공휴일 동기화 실패, 기본 데이터 생성: {}", e.getMessage());
            createDefaultHolidays(year, countryCode);
        }
    }

    /**
     * 공공데이터 API에서 공휴일 정보 동기화
     */
    @Transactional
    public void syncHolidaysFromPublicAPI(int year, String countryCode) {
        log.info("{}년 {}국가 공휴일 공공 API 동기화 시작", year, countryCode);

        try {
            // 한국 공공데이터만 지원
            if (!"KR".equals(countryCode)) {
                log.warn("한국 이외의 국가는 공공데이터 API를 지원하지 않습니다: {}", countryCode);
                createDefaultHolidays(year, countryCode);
                return;
            }

            // 공공데이터 API 호출
            List<HolidayDTO> apiHolidays = fetchHolidaysFromPublicAPI(year);

            if (apiHolidays.isEmpty()) {
                log.warn("공공 API에서 {}년 공휴일을 가져오지 못했습니다. 기본 데이터를 생성합니다.", year);
                createDefaultKoreanHolidays(year);
                return;
            }

            // DB에 저장
            int savedCount = 0;
            for (HolidayDTO holidayDTO : apiHolidays) {
                if (!holidayRepository.existsByNameAndHolidayDateAndCountryCode(
                        holidayDTO.getName(), holidayDTO.getHolidayDate(), countryCode)) {

                    Holiday holiday = holidayDTO.toEntity();
                    holiday.setCountryCode(countryCode);
                    holidayRepository.save(holiday);
                    savedCount++;
                }
            }

            log.info("공공 API에서 {}년 공휴일 {}개를 성공적으로 동기화했습니다.", year, savedCount);

        } catch (Exception e) {
            log.error("공공 API 동기화 실패 - 연도: {}, 오류: {}", year, e.getMessage());
            log.info("API 실패로 인해 기본 공휴일 데이터를 생성합니다.");
            createDefaultKoreanHolidays(year);
        }
    }

    /**
     * 한국 행정안전부 특일정보 API 호출
     */
    private List<HolidayDTO> fetchHolidaysFromPublicAPI(int year) {
        List<HolidayDTO> holidays = new ArrayList<>();
        int maxRetries = publicDataConfig.getMaxRetryAttempts();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("공공데이터 API 호출 시도 {}/{} - 연도: {}", attempt, maxRetries, year);

                // API URL 구성
                String url = buildPublicApiUrl(year);
                log.debug("API URL: {}", url);

                // API 호출
                String response = publicDataRestTemplate.getForObject(url, String.class);

                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("API 응답이 비어있습니다.");
                }

                // XML 응답 파싱
                holidays = parsePublicApiResponse(response, year);

                if (!holidays.isEmpty()) {
                    log.info("공공 API에서 {}년 공휴일 {}개를 성공적으로 가져왔습니다.", year, holidays.size());
                    break;
                }

            } catch (Exception e) {
                log.warn("공공 API 호출 실패 (시도 {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(publicDataConfig.getRetryDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("공공 API 호출 최대 재시도 횟수 초과: {}", e.getMessage());
                    throw new RuntimeException("공공데이터 API 호출 실패", e);
                }
            }
        }

        return holidays;
    }

    /**
     * 공공데이터 API URL 생성
     */
    private String buildPublicApiUrl(int year) {
        return String.format("%s/getRestDeInfo?serviceKey=%s&solYear=%d&_type=json",
                publicDataConfig.getHolidayApiBaseUrl(),
                publicDataConfig.getServiceKey(),
                year);
    }

    /**
     * 공공데이터 API 응답 파싱 (JSON 형식)
     */
    private List<HolidayDTO> parsePublicApiResponse(String response, int year) {
        List<HolidayDTO> holidays = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode responseNode = rootNode.path("response");
            JsonNode bodyNode = responseNode.path("body");
            JsonNode itemsNode = bodyNode.path("items");

            // 응답 상태 확인
            JsonNode headerNode = responseNode.path("header");
            String resultCode = headerNode.path("resultCode").asText();

            if (!"00".equals(resultCode)) {
                String resultMsg = headerNode.path("resultMsg").asText();
                throw new RuntimeException("API 오류 - 코드: " + resultCode + ", 메시지: " + resultMsg);
            }

            // 데이터가 배열인지 객체인지 확인
            JsonNode itemNode = itemsNode.path("item");
            if (itemNode.isArray()) {
                for (JsonNode holiday : itemNode) {
                    HolidayDTO holidayDTO = parseHolidayItem(holiday, year);
                    if (holidayDTO != null) {
                        holidays.add(holidayDTO);
                    }
                }
            } else if (!itemNode.isMissingNode()) {
                HolidayDTO holidayDTO = parseHolidayItem(itemNode, year);
                if (holidayDTO != null) {
                    holidays.add(holidayDTO);
                }
            }

        } catch (Exception e) {
            log.error("공공 API 응답 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("API 응답 파싱 오류", e);
        }

        return holidays;
    }

    /**
     * 개별 공휴일 항목 파싱
     */
    private HolidayDTO parseHolidayItem(JsonNode holidayNode, int year) {
        try {
            String dateName = holidayNode.path("dateName").asText();
            String locdate = holidayNode.path("locdate").asText();
            String isHoliday = holidayNode.path("isHoliday").asText("Y");

            if (dateName.isEmpty() || locdate.isEmpty()) {
                return null;
            }

            // 날짜 파싱 (YYYYMMDD 형식)
            LocalDate holidayDate = LocalDate.parse(locdate, DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 공휴일 타입 결정
            Holiday.HolidayType holidayType = determineHolidayType(dateName);

            // 색상 결정
            String color = getColorByType(holidayType);

            return HolidayDTO.builder()
                    .name(dateName)
                    .holidayDate(holidayDate)
                    .countryCode("KR")
                    .holidayType(holidayType)
                    .description("공공데이터에서 동기화된 " + dateName)
                    .isRecurring(isRecurringHoliday(dateName))
                    .color(color)
                    .build();

        } catch (Exception e) {
            log.warn("공휴일 항목 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 공휴일명으로 타입 결정
     */
    private Holiday.HolidayType determineHolidayType(String holidayName) {
        // 국경일
        if (holidayName.contains("삼일절") || holidayName.contains("광복절") ||
                holidayName.contains("개천절") || holidayName.contains("한글날")) {
            return Holiday.HolidayType.NATIONAL;
        }

        // 기념일
        if (holidayName.contains("현충일")) {
            return Holiday.HolidayType.MEMORIAL;
        }

        // 대체공휴일
        if (holidayName.contains("대체") || holidayName.contains("임시")) {
            return Holiday.HolidayType.SUBSTITUTE;
        }

        // 기본은 공휴일
        return Holiday.HolidayType.PUBLIC;
    }

    /**
     * 타입별 색상 반환
     */
    private String getColorByType(Holiday.HolidayType type) {
        return switch (type) {
            case NATIONAL -> "#4285F4";    // 파란색 (국경일)
            case PUBLIC -> "#FF6B6B";      // 빨간색 (공휴일)
            case SUBSTITUTE -> "#FF9800";  // 주황색 (대체공휴일)
            case TRADITIONAL -> null;
            case RELIGIOUS -> null;
            case MEMORIAL -> "#9C27B0";    // 보라색 (기념일)
            case ANNIVERSARY -> "#607D8B"; // 회색 (기타)
        };
    }

    /**
     * 매년 반복되는 공휴일인지 확인
     */
    private boolean isRecurringHoliday(String holidayName) {
        // 음력 기반 공휴일은 매년 날짜가 다름
        return !holidayName.contains("설날") &&
                !holidayName.contains("추석") &&
                !holidayName.contains("석가탄신일") &&
                !holidayName.contains("대체");
    }

    /**
     * 기본 한국 공휴일 생성 (API 실패 시 폴백)
     */
    @Transactional
    public void createDefaultKoreanHolidays(int year) {
        log.info("{}년 기본 한국 공휴일 데이터 생성", year);

        Map<String, HolidayInfo> defaultHolidays = getDefaultKoreanHolidayMap();

        int createdCount = 0;
        for (Map.Entry<String, HolidayInfo> entry : defaultHolidays.entrySet()) {
            String dateStr = entry.getKey();
            HolidayInfo info = entry.getValue();

            try {
                LocalDate holidayDate = LocalDate.of(year,
                        Integer.parseInt(dateStr.split("-")[0]),
                        Integer.parseInt(dateStr.split("-")[1]));

                if (!holidayRepository.existsByNameAndHolidayDateAndCountryCode(
                        info.name, holidayDate, "KR")) {

                    HolidayDTO holidayDTO = HolidayDTO.builder()
                            .name(info.name)
                            .holidayDate(holidayDate)
                            .countryCode("KR")
                            .holidayType(info.type)
                            .description(info.description)
                            .isRecurring(true)
                            .color(getColorByType(info.type))
                            .build();

                    Holiday holiday = holidayDTO.toEntity();
                    holidayRepository.save(holiday);
                    createdCount++;
                }
            } catch (Exception e) {
                log.warn("기본 공휴일 생성 실패 - {}: {}", info.name, e.getMessage());
            }
        }

        log.info("{}년 기본 공휴일 {}개 생성 완료", year, createdCount);
    }

    /**
     * 기본 한국 공휴일 정보
     */
    private Map<String, HolidayInfo> getDefaultKoreanHolidayMap() {
        Map<String, HolidayInfo> holidays = new LinkedHashMap<>();

        holidays.put("01-01", new HolidayInfo("신정", Holiday.HolidayType.PUBLIC, "새해 첫날"));
        holidays.put("03-01", new HolidayInfo("삼일절", Holiday.HolidayType.NATIONAL, "3·1 독립운동 기념일"));
        holidays.put("05-05", new HolidayInfo("어린이날", Holiday.HolidayType.PUBLIC, "어린이날"));
        holidays.put("06-06", new HolidayInfo("현충일", Holiday.HolidayType.MEMORIAL, "호국영령을 추모하는 날"));
        holidays.put("08-15", new HolidayInfo("광복절", Holiday.HolidayType.NATIONAL, "일제강점기 해방 기념일"));
        holidays.put("10-03", new HolidayInfo("개천절", Holiday.HolidayType.NATIONAL, "단군왕검이 고조선을 건국한 날"));
        holidays.put("10-09", new HolidayInfo("한글날", Holiday.HolidayType.NATIONAL, "한글 창제를 기념하는 날"));
        holidays.put("12-25", new HolidayInfo("크리스마스", Holiday.HolidayType.PUBLIC, "예수 그리스도의 탄생을 기념하는 날"));

        return holidays;
    }

    /**
     * 다른 국가의 기본 공휴일 (확장성을 위해)
     */
    @Transactional
    public void createDefaultHolidays(int year, String countryCode) {
        if ("KR".equals(countryCode)) {
            createDefaultKoreanHolidays(year);
        } else {
            log.warn("{}국가의 기본 공휴일은 지원하지 않습니다.", countryCode);
        }
    }

    /**
     * 공휴일/국경일 등록
     */
    @Transactional
    public HolidayDTO createHoliday(HolidayDTO holidayDTO) {
        log.info("공휴일 등록: {}", holidayDTO.getName());

        // 중복 검사
        List<Holiday> existingHolidays = holidayRepository.findByHolidayDateAndCountryCodeOrderByHolidayTypeAsc(
                holidayDTO.getHolidayDate(),
                holidayDTO.getCountryCode() != null ? holidayDTO.getCountryCode() : "KR");

        boolean alreadyExists = existingHolidays.stream()
                .anyMatch(h -> h.getName().equals(holidayDTO.getName()));

        if (alreadyExists) {
            throw new IllegalArgumentException("이미 등록된 공휴일입니다: " + holidayDTO.getName());
        }

        Holiday holiday = holidayDTO.toEntity();
        Holiday savedHoliday = holidayRepository.save(holiday);

        log.info("공휴일 등록 완료 - ID: {}, 이름: {}", savedHoliday.getId(), savedHoliday.getName());

        return HolidayDTO.fromEntity(savedHoliday);
    }

    /**
     * 공휴일/국경일 수정
     */
    @Transactional
    public HolidayDTO updateHoliday(Long id, HolidayDTO holidayDTO) {
        log.info("공휴일 수정 - ID: {}", id);

        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공휴일입니다: " + id));

        // 업데이트
        holiday.setName(holidayDTO.getName());
        holiday.setHolidayDate(holidayDTO.getHolidayDate());
        holiday.setCountryCode(holidayDTO.getCountryCode());
        holiday.setHolidayType(holidayDTO.getHolidayType());
        holiday.setDescription(holidayDTO.getDescription());
        holiday.setIsRecurring(holidayDTO.getIsRecurring());
        holiday.setColor(holidayDTO.getColor());

        Holiday updatedHoliday = holidayRepository.save(holiday);

        log.info("공휴일 수정 완료 - ID: {}, 이름: {}", updatedHoliday.getId(), updatedHoliday.getName());

        return HolidayDTO.fromEntity(updatedHoliday);
    }

    /**
     * 공휴일/국경일 삭제
     */
    @Transactional
    public void deleteHoliday(Long id) {
        log.info("공휴일 삭제 - ID: {}", id);

        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 공휴일입니다: " + id);
        }

        holidayRepository.deleteById(id);

        log.info("공휴일 삭제 완료 - ID: {}", id);
    }

    /**
     * 연도별 공휴일 동기화 (관리자용)
     */
    @Async
    @Transactional
    public void syncHolidaysByYearAsync(int year) {
        log.info("비동기 공휴일 동기화 시작 - 연도: {}", year);
        syncHolidaysFromPublicAPI(year, "KR");
        log.info("비동기 공휴일 동기화 완료 - 연도: {}", year);
    }

    /**
     * 공휴일 통계 조회
     */
    public Map<String, Object> getHolidayStatistics(int year, String countryCode) {
        List<Holiday> holidays = holidayRepository.findByYearAndCountryCode(year, countryCode);

        Map<String, Object> stats = new HashMap<>();
        stats.put("year", year);
        stats.put("countryCode", countryCode);
        stats.put("totalCount", holidays.size());

        // 타입별 통계
        Map<Holiday.HolidayType, Long> typeStats = holidays.stream()
                .collect(Collectors.groupingBy(Holiday::getHolidayType, Collectors.counting()));
        stats.put("typeStatistics", typeStats);

        // 월별 통계
        Map<Integer, Long> monthStats = holidays.stream()
                .collect(Collectors.groupingBy(h -> h.getHolidayDate().getMonthValue(), Collectors.counting()));
        stats.put("monthStatistics", monthStats);

        return stats;
    }

    /**
     * 공휴일 정보 내부 클래스
     */
    private static class HolidayInfo {
        final String name;
        final Holiday.HolidayType type;
        final String description;

        HolidayInfo(String name, Holiday.HolidayType type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }
    }
}
