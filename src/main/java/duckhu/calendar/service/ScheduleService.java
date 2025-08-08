package duckhu.calendar.service;

import duckhu.calendar.dto.ScheduleRequestDto;
import duckhu.calendar.dto.ScheduleResponseDto;
import duckhu.calendar.entity.Schedule;
import duckhu.calendar.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 향상된 일정 서비스
 * 이미지, 링크, 추천 기능 등을 포함한 향상된 일정 관리 비즈니스 로직
 */
@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    // 기본 색상 배열 (확장됨)
    private final String[] DEFAULT_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#F4A460", "#87CEEB", "#98D8C8", "#FFB6C1",
            "#FFA07A", "#20B2AA", "#9370DB", "#3CB371", "#FF7F50",
            "#FF9800", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF5722"
    };

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 정렬 옵션을 포함한 모든 일정 조회
     * @param sortBy 정렬 기준 (date, priority, views, created)
     * @return 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getAllSchedules(String sortBy) {
        Sort sort = createSort(sortBy);
        List<Schedule> schedules = scheduleRepository.findAll(sort);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 기본 모든 일정 조회 (날짜순 정렬)
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getAllSchedules() {
        return getAllSchedules("date");
    }

    /**
     * ID로 일정 조회 및 조회수 증가
     * @param id 일정 ID
     * @return 일정 정보
     */
    @Transactional
    public ScheduleResponseDto getScheduleByIdWithViewCount(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다. ID: " + id));

        // 조회수 증가
        schedule.incrementViewCount();
        scheduleRepository.save(schedule);

        return ScheduleResponseDto.from(schedule);
    }

    /**
     * 조회수 증가 없이 일정 조회
     */
    @Transactional(readOnly = true)
    public ScheduleResponseDto getScheduleById(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다. ID: " + id));
        return ScheduleResponseDto.from(schedule);
    }

    /**
     * 새 일정 생성
     * @param requestDto 일정 생성 요청 데이터
     * @return 생성된 일정 정보
     */
    public ScheduleResponseDto createSchedule(ScheduleRequestDto requestDto) {
        // 유효성 검사
        validateScheduleRequest(requestDto);

        // DTO를 Entity로 변환
        Schedule schedule = convertToEntity(requestDto);

        // 색상이 없으면 기본 색상 할당
        if (schedule.getColor() == null || schedule.getColor().isEmpty()) {
            schedule.setColor(getRandomColor());
        }

        // 링크 제목 자동 생성
        if (schedule.getLinks() != null && !schedule.getLinks().isEmpty()) {
            generateLinkTitlesIfNeeded(schedule);
        }

        // 데이터베이스에 저장
        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponseDto.from(savedSchedule);
    }

    /**
     * 일정 수정
     * @param id 수정할 일정 ID
     * @param requestDto 수정 요청 데이터
     * @return 수정된 일정 정보
     */
    public ScheduleResponseDto updateSchedule(Long id, ScheduleRequestDto requestDto) {
        // 기존 일정 조회
        Schedule existingSchedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("수정할 일정을 찾을 수 없습니다. ID: " + id));

        // 유효성 검사
        validateScheduleRequest(requestDto);

        // 기존 일정 정보 업데이트
        updateScheduleEntity(existingSchedule, requestDto);

        // 데이터베이스에 저장
        Schedule updatedSchedule = scheduleRepository.save(existingSchedule);
        return ScheduleResponseDto.from(updatedSchedule);
    }

    /**
     * 일정 삭제
     * @param id 삭제할 일정 ID
     */
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("삭제할 일정을 찾을 수 없습니다. ID: " + id));

        scheduleRepository.delete(schedule);
    }

    /**
     * 특정 날짜의 일정 조회
     * @param date 조회할 날짜
     * @return 해당 날짜의 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getSchedulesByDate(LocalDate date) {
        List<Schedule> schedules = scheduleRepository.findSchedulesByDate(date);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜 범위의 일정 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 범위의 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Schedule> schedules = scheduleRepository.findSchedulesByDateRange(startDate, endDate);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 월의 일정 조회
     * @param year 연도
     * @param month 월 (1-12)
     * @return 해당 월의 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getSchedulesByMonth(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<Schedule> schedules = scheduleRepository.findSchedulesByMonth(year, month, monthStart, monthEnd);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 향상된 일정 검색 (제목 + 카테고리)
     * @param title 검색할 제목
     * @param category 카테고리 필터
     * @return 검색된 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> searchSchedules(String title, String category) {
        List<Schedule> schedules;

        if (title != null && !title.trim().isEmpty() && category != null && !category.trim().isEmpty()) {
            // 제목과 카테고리 모두 검색
            schedules = scheduleRepository.findByTitleContainingIgnoreCaseAndCategoryOrderByStartDateDesc(
                    title.trim(), category.trim());
        } else if (title != null && !title.trim().isEmpty()) {
            // 제목만 검색
            schedules = scheduleRepository.findByTitleContainingIgnoreCaseOrderByStartDateDesc(title.trim());
        } else if (category != null && !category.trim().isEmpty()) {
            // 카테고리만 검색
            schedules = scheduleRepository.findByCategoryOrderByStartDateDesc(category.trim());
        } else {
            // 전체 목록 반환
            schedules = scheduleRepository.findAllByOrderByStartDateDesc();
        }

        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 제목으로만 검색 (기존 호환성)
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> searchSchedulesByTitle(String title) {
        return searchSchedules(title, null);
    }

    /**
     * 오늘의 일정 조회
     * @return 오늘의 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getTodaySchedules() {
        return getSchedulesByDate(LocalDate.now());
    }

    /**
     * 다가오는 일정 조회
     * @param days 몇 일 후까지 조회할지
     * @return 다가오는 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getUpcomingSchedules(int days) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);

        return getSchedulesByDateRange(today, futureDate);
    }

    /**
     * 추천 이벤트 토글
     * @param id 일정 ID
     * @param isFeatured 추천 여부
     * @return 업데이트된 일정 정보
     */
    public ScheduleResponseDto toggleFeatured(Long id, Boolean isFeatured) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다. ID: " + id));

        schedule.setIsFeatured(isFeatured);
        Schedule savedSchedule = scheduleRepository.save(schedule);

        return ScheduleResponseDto.from(savedSchedule);
    }

    /**
     * 조회수 증가
     * @param id 일정 ID
     */
    public void incrementViewCount(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다. ID: " + id));

        schedule.incrementViewCount();
        scheduleRepository.save(schedule);
    }

    /**
     * 추천 이벤트 목록 조회
     * @return 추천 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getFeaturedSchedules() {
        return getFeaturedSchedules(10); // 기본 10개
    }

    /**
     * 추천 이벤트 목록 조회 (개수 제한)
     * @param limit 조회할 개수
     * @return 추천 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getFeaturedSchedules(int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.ASC, "startDate")
                        .and(Sort.by(Sort.Direction.DESC, "viewCount")));

        List<Schedule> schedules = scheduleRepository.findByIsFeaturedTrueOrderByStartDateAscViewCountDesc(pageable);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 인기 이벤트 목록 조회 (조회수 기준)
     * @param limit 조회할 개수
     * @return 인기 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getPopularSchedules(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"));
        List<Schedule> schedules = scheduleRepository.findByViewCountGreaterThanOrderByViewCountDesc(0, pageable);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 최근 추가된 이벤트 조회
     * @param limit 조회할 개수
     * @return 최근 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getRecentSchedules(int limit) {
        List<Schedule> schedules = scheduleRepository.findRecentSchedules(limit);
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 일정 통계 조회
     * @return 통계 정보 맵
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getScheduleStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 전체 통계
        long totalSchedules = scheduleRepository.count();
        long featuredSchedules = scheduleRepository.countByIsFeaturedTrue();
        long todaySchedules = scheduleRepository.countTodaySchedules(LocalDate.now());

        stats.put("totalSchedules", totalSchedules);
        stats.put("featuredSchedules", featuredSchedules);
        stats.put("todaySchedules", todaySchedules);

        // 우선순위별 통계
        List<Object[]> priorityStats = scheduleRepository.countByPriority();
        Map<String, Long> priorityMap = new HashMap<>();
        for (Object[] row : priorityStats) {
            Integer priority = (Integer) row[0];
            Long count = (Long) row[1];
            String priorityText = switch (priority) {
                case 1 -> "높음";
                case 2 -> "중간";
                case 3 -> "낮음";
                default -> "기타";
            };
            priorityMap.put(priorityText, count);
        }
        stats.put("priorityStats", priorityMap);

        // 월별 통계 (최근 6개월)
        Map<String, Long> monthlyStats = new HashMap<>();
        LocalDate startMonth = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = startMonth.plusMonths(i);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            long count = scheduleRepository.countByDateRange(monthStart, monthEnd);
            monthlyStats.put(monthStart.getMonth().toString(), count);
        }
        stats.put("monthlyStats", monthlyStats);

        // 조회수 통계
        Long totalViews = scheduleRepository.sumAllViewCounts();
        stats.put("totalViews", totalViews != null ? totalViews : 0);

        return stats;
    }

    // === 유틸리티 메서드들 ===

    /**
     * 정렬 조건 생성
     */
    private Sort createSort(String sortBy) {
        if (sortBy == null) {
            sortBy = "date";
        }

        return switch (sortBy.toLowerCase()) {
            case "priority" -> Sort.by(Sort.Direction.ASC, "priority")
                    .and(Sort.by(Sort.Direction.ASC, "startDate"));
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount")
                    .and(Sort.by(Sort.Direction.ASC, "startDate"));
            case "created" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.ASC, "startDate")
                    .and(Sort.by(Sort.Direction.ASC, "priority"));
        };
    }

    /**
     * 일정 요청 데이터 유효성 검사
     */
    private void validateScheduleRequest(ScheduleRequestDto requestDto) {
        // 기본 날짜/시간 검증
        if (!requestDto.isValidDateRange()) {
            throw new RuntimeException("종료일은 시작일보다 늦거나 같아야 합니다.");
        }

        if (!requestDto.isValidTimeRange()) {
            throw new RuntimeException("같은 날짜에서는 종료 시간이 시작 시간보다 늦어야 합니다.");
        }

        // 이미지 URL 검증
        if (!requestDto.areImagesValid()) {
            throw new RuntimeException("이미지 URL이 올바르지 않거나 개수 제한을 초과했습니다. (최대 3개)");
        }

        // 링크 URL 검증
        if (!requestDto.areLinksValid()) {
            throw new RuntimeException("링크 URL이 올바르지 않거나 개수 제한을 초과했습니다. (최대 2개)");
        }

        // 과거 날짜 경고
        if (requestDto.getStartDate().isBefore(LocalDate.now().minusDays(1))) {
            System.out.println("경고: 과거 날짜의 일정이 생성되었습니다. " + requestDto.getStartDate());
        }
    }

    /**
     * DTO를 Entity로 변환
     */
    private Schedule convertToEntity(ScheduleRequestDto requestDto) {
        Schedule schedule = new Schedule();
        schedule.setTitle(requestDto.getTitle());
        schedule.setStartDate(requestDto.getStartDate());
        schedule.setEndDate(requestDto.getEndDate());
        schedule.setStartTime(requestDto.getStartTime());
        schedule.setEndTime(requestDto.getEndTime());
        schedule.setDescription(requestDto.getDescription());
        schedule.setPriority(requestDto.getPriority());
        schedule.setColor(requestDto.getColor());
        schedule.setCategory(requestDto.getCategory());

        // 새로운 필드들
        schedule.setImages(requestDto.getImages());
        schedule.setLinks(requestDto.getLinks());
        schedule.setLinkTitles(requestDto.getLinkTitles());
        schedule.setIsFeatured(requestDto.getIsFeatured() != null ? requestDto.getIsFeatured() : false);

        return schedule;
    }

    /**
     * 기존 Entity를 DTO 데이터로 업데이트
     */
    private void updateScheduleEntity(Schedule schedule, ScheduleRequestDto requestDto) {
        schedule.setTitle(requestDto.getTitle());
        schedule.setStartDate(requestDto.getStartDate());
        schedule.setEndDate(requestDto.getEndDate());
        schedule.setStartTime(requestDto.getStartTime());
        schedule.setEndTime(requestDto.getEndTime());
        schedule.setDescription(requestDto.getDescription());
        schedule.setPriority(requestDto.getPriority());

        // 색상이 제공된 경우에만 업데이트
        if (requestDto.getColor() != null && !requestDto.getColor().isEmpty()) {
            schedule.setColor(requestDto.getColor());
        }

        // 카테고리 업데이트
        schedule.setCategory(requestDto.getCategory());

        // 새로운 필드들 업데이트
        schedule.setImages(requestDto.getImages());
        schedule.setLinks(requestDto.getLinks());
        schedule.setLinkTitles(requestDto.getLinkTitles());

        // 관리자가 아닌 경우 추천 설정 변경 불가 (컨트롤러에서 처리)
        if (requestDto.getIsFeatured() != null) {
            schedule.setIsFeatured(requestDto.getIsFeatured());
        }

        // 링크 제목 자동 생성
        if (schedule.getLinks() != null && !schedule.getLinks().isEmpty()) {
            generateLinkTitlesIfNeeded(schedule);
        }
    }

    /**
     * 링크 제목 자동 생성
     */
    private void generateLinkTitlesIfNeeded(Schedule schedule) {
        if (schedule.getLinkTitles() == null || schedule.getLinkTitles().size() < schedule.getLinks().size()) {
            List<String> linkTitles = schedule.getLinkTitles() != null
                    ? new java.util.ArrayList<>(schedule.getLinkTitles())
                    : new java.util.ArrayList<>();

            while (linkTitles.size() < schedule.getLinks().size()) {
                linkTitles.add("링크 " + (linkTitles.size() + 1));
            }
            schedule.setLinkTitles(linkTitles);
        }
    }

    /**
     * 랜덤 색상 선택
     */
    private String getRandomColor() {
        int randomIndex = (int) (Math.random() * DEFAULT_COLORS.length);
        return DEFAULT_COLORS[randomIndex];
    }
}
