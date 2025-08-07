package duckhu.calendar.repository;

import duckhu.calendar.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 Repository
 * JPA를 사용한 데이터베이스 접근 계층
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 특정 날짜에 해당하는 일정 조회 (날짜 범위 겹치는 것들)
     * @param date 조회할 날짜
     * @return 해당 날짜에 포함되는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.startDate <= :date AND s.endDate >= :date ORDER BY s.priority ASC, s.startTime ASC")
    List<Schedule> findSchedulesByDate(@Param("date") LocalDate date);

    /**
     * 특정 날짜 범위에 해당하는 일정 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 범위에 겹치는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "(s.startDate <= :endDate AND s.endDate >= :startDate) " +
            "ORDER BY s.startDate ASC, s.priority ASC, s.startTime ASC")
    List<Schedule> findSchedulesByDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 특정 월의 모든 일정 조회
     * @param year 연도
     * @param month 월 (1-12)
     * @return 해당 월에 포함되는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "YEAR(s.startDate) = :year AND MONTH(s.startDate) = :month " +
            "OR YEAR(s.endDate) = :year AND MONTH(s.endDate) = :month " +
            "OR (s.startDate <= :monthStart AND s.endDate >= :monthEnd) " +
            "ORDER BY s.startDate ASC, s.priority ASC")
    List<Schedule> findSchedulesByMonth(@Param("year") int year,
                                        @Param("month") int month,
                                        @Param("monthStart") LocalDate monthStart,
                                        @Param("monthEnd") LocalDate monthEnd);

    /**
     * 우선순위별 일정 조회
     * @param priority 우선순위 (1: 높음, 2: 중간, 3: 낮음)
     * @return 해당 우선순위의 일정 목록
     */
    List<Schedule> findByPriorityOrderByStartDateAscStartTimeAsc(Integer priority);

    /**
     * 제목으로 일정 검색 (부분 일치)
     * @param title 검색할 제목
     * @return 제목에 포함된 일정 목록
     */
    List<Schedule> findByTitleContainingIgnoreCaseOrderByStartDateDesc(String title);

    /**
     * 특정 날짜 이후의 일정 조회 (upcoming schedules)
     * @param date 기준 날짜
     * @return 해당 날짜 이후의 일정 목록
     */
    List<Schedule> findByStartDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(LocalDate date);

    /**
     * 종일 일정 조회
     * @return 시간이 설정되지 않은 종일 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.startTime IS NULL AND s.endTime IS NULL ORDER BY s.startDate ASC")
    List<Schedule> findAllDaySchedules();

    /**
     * 시간이 설정된 일정 조회
     * @return 시간이 설정된 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.startTime IS NOT NULL ORDER BY s.startDate ASC, s.startTime ASC")
    List<Schedule> findTimedSchedules();

    /**
     * 생성일 기준 최근 일정 조회
     * @param limit 조회할 개수
     * @return 최근 생성된 일정 목록
     */
    @Query(value = "SELECT * FROM schedules ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Schedule> findRecentSchedules(@Param("limit") int limit);

    /**
     * 오늘 일정 개수 조회
     * @param today 오늘 날짜
     * @return 오늘 일정 개수
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.startDate <= :today AND s.endDate >= :today")
    long countTodaySchedules(@Param("today") LocalDate today);

    /**
     * 우선순위별 일정 개수 조회
     * @return 우선순위별 일정 개수
     */
    @Query("SELECT s.priority, COUNT(s) FROM Schedule s GROUP BY s.priority ORDER BY s.priority")
    List<Object[]> countByPriority();
}
