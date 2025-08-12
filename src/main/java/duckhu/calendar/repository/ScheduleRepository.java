package duckhu.calendar.repository;

import duckhu.calendar.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 특정 날짜에 해당하는 일정 조회 (날짜 범위 겹치는 것들)
     */
    @Query("SELECT s FROM Schedule s WHERE s.startDate <= :date AND s.endDate >= :date " +
            "ORDER BY s.isFeatured DESC, s.priority ASC, s.startTime ASC")
    List<Schedule> findSchedulesByDate(@Param("date") LocalDate date);

    /**
     * 특정 날짜 범위에 해당하는 일정 조회 - PostgreSQL 호환
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "(s.startDate <= :endDate AND s.endDate >= :startDate) " +
            "ORDER BY s.isFeatured DESC, s.startDate ASC, s.priority ASC, s.startTime ASC")
    List<Schedule> findSchedulesByDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 특정 월의 모든 일정 조회 - PostgreSQL EXTRACT 함수 사용
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "(EXTRACT(YEAR FROM s.startDate) = :year AND EXTRACT(MONTH FROM s.startDate) = :month) " +
            "OR (EXTRACT(YEAR FROM s.endDate) = :year AND EXTRACT(MONTH FROM s.endDate) = :month) " +
            "OR (s.startDate <= :monthStart AND s.endDate >= :monthEnd) " +
            "ORDER BY s.isFeatured DESC, s.startDate ASC, s.priority ASC")
    List<Schedule> findSchedulesByMonth(@Param("year") int year,
                                        @Param("month") int month,
                                        @Param("monthStart") LocalDate monthStart,
                                        @Param("monthEnd") LocalDate monthEnd);

    /**
     * 우선순위별 일정 조회
     */
    List<Schedule> findByPriorityOrderByStartDateAscStartTimeAsc(Integer priority);

    /**
     * 제목으로 일정 검색 (부분 일치)
     */
    List<Schedule> findByTitleContainingIgnoreCaseOrderByStartDateDesc(String title);

    /**
     * 카테고리별 일정 검색
     */
    List<Schedule> findByCategoryOrderByStartDateDesc(String category);

    /**
     * 제목과 카테고리로 일정 검색
     */
    List<Schedule> findByTitleContainingIgnoreCaseAndCategoryOrderByStartDateDesc(String title, String category);

    /**
     * 특정 날짜 이후의 일정 조회
     */
    List<Schedule> findByStartDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(LocalDate date);

    /**
     * 전체 일정 조회 (생성일 역순)
     */
    List<Schedule> findAllByOrderByStartDateDesc();

    /**
     * 생성일 기준 최근 일정 조회 - PostgreSQL 호환
     */
    @Query("SELECT s FROM Schedule s ORDER BY s.createdAt DESC")
    List<Schedule> findRecentSchedules(Pageable pageable);

    /**
     * 오늘 일정 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.startDate <= :today AND s.endDate >= :today")
    long countTodaySchedules(@Param("today") LocalDate today);

    /**
     * 우선순위별 일정 개수 조회
     */
    @Query("SELECT s.priority, COUNT(s) FROM Schedule s GROUP BY s.priority ORDER BY s.priority")
    List<Object[]> countByPriority();

    /**
     * 추천 이벤트 조회 (전체)
     */
    List<Schedule> findByIsFeaturedTrueOrderByStartDateAsc();

    /**
     * 추천 이벤트 조회 (페이징 지원)
     */
    List<Schedule> findByIsFeaturedTrueOrderByStartDateAsc(Pageable pageable);

    /**
     * 추천 이벤트 개수 조회
     */
    long countByIsFeaturedTrue();

    /**
     * 조회수 기준 인기 이벤트 조회
     */
    List<Schedule> findAllByOrderByViewCountDesc(Pageable pageable);

    /**
     * 생성일 기준 최근 이벤트 조회
     */
    List<Schedule> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 전체 조회수 합계 조회
     */
    @Query("SELECT SUM(s.viewCount) FROM Schedule s")
    Long sumAllViewCounts();

    /**
     * 조회수 높은 일정 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.viewCount > 0 " +
            "ORDER BY s.viewCount DESC, s.startDate DESC")
    List<Schedule> findTopViewedSchedules(Pageable pageable);

    /**
     * 특정 기간의 일정 개수 조회 (통계용)
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE " +
            "s.startDate >= :startDate AND s.startDate <= :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
