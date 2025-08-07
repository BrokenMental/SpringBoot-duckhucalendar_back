package duckhu.calendar.service;

import duckhu.calendar.dto.ScheduleRequestDto;
import duckhu.calendar.dto.ScheduleResponseDto;
import duckhu.calendar.entity.Schedule;
import duckhu.calendar.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 서비스
 * 일정 관련 비즈니스 로직을 처리하는 서비스 계층
 */
@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    // 기본 색상 배열
    private final String[] DEFAULT_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#F4A460", "#87CEEB", "#98D8C8", "#FFB6C1",
            "#FFA07A", "#20B2AA", "#9370DB", "#3CB371", "#FF7F50"
    };

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 모든 일정 조회
     * @return 모든 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getAllSchedules() {
        List<Schedule> schedules = scheduleRepository.findAll();
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 ID의 일정 조회
     * @param id 일정 ID
     * @return 일정 정보
     * @throws RuntimeException 일정을 찾을 수 없는 경우
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
     * @throws RuntimeException 유효성 검사 실패 시
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

        // 데이터베이스에 저장
        Schedule savedSchedule = scheduleRepository.save(schedule);

        return ScheduleResponseDto.from(savedSchedule);
    }

    /**
     * 일정 수정
     * @param id 수정할 일정 ID
     * @param requestDto 수정 요청 데이터
     * @return 수정된 일정 정보
     * @throws RuntimeException 일정을 찾을 수 없거나 유효성 검사 실패 시
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
     * @throws RuntimeException 일정을 찾을 수 없는 경우
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
     * 제목으로 일정 검색
     * @param title 검색할 제목
     * @return 검색된 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> searchSchedulesByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return getAllSchedules();
        }

        List<Schedule> schedules = scheduleRepository.findByTitleContainingIgnoreCaseOrderByStartDateDesc(title.trim());
        return schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());
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

    // === 유틸리티 메서드들 ===

    /**
     * 일정 요청 데이터 유효성 검사
     */
    private void validateScheduleRequest(ScheduleRequestDto requestDto) {
        // 날짜 순서 검증
        if (!requestDto.isValidDateRange()) {
            throw new RuntimeException("종료일은 시작일보다 늦거나 같아야 합니다.");
        }

        // 시간 순서 검증
        if (!requestDto.isValidTimeRange()) {
            throw new RuntimeException("같은 날짜에서는 종료 시간이 시작 시간보다 늦어야 합니다.");
        }

        // 과거 날짜 검증 (선택사항)
        if (requestDto.getStartDate().isBefore(LocalDate.now().minusDays(1))) {
            // 경고만 로그로 남기고 저장은 허용
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
    }

    /**
     * 랜덤 색상 선택
     */
    private String getRandomColor() {
        int randomIndex = (int) (Math.random() * DEFAULT_COLORS.length);
        return DEFAULT_COLORS[randomIndex];
    }
}
