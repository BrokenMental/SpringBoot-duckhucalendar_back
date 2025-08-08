package duckhu.calendar.repository;

import duckhu.calendar.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 향상된 일정 Repository
 * 이미지, 링크, 추천 기능 등을 포함한 향상된 데이터베이스 접근 계층
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // === 기존 메서드들 ===

    /**
     * 특정 날짜에 해당하는 일정 조회 (날짜 범위 겹치는 것들)
     * @param date 조회할 날짜
     * @return 해당 날짜에 포함되는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.startDate <= :date AND s.endDate >= :date " +
            "ORDER BY s.isFeatured DESC, s.priority ASC, s.startTime ASC")
    List<Schedule> findSchedulesByDate(@Param("date") LocalDate date);

    /**
     * 특정 날짜 범위에 해당하는 일정 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 범위에 겹치는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "(s.startDate <= :endDate AND s.endDate >= :startDate) " +
            "ORDER BY s.isFeatured DESC, s.startDate ASC, s.priority ASC, s.startTime ASC")
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
            "ORDER BY s.isFeatured DESC, s.startDate ASC, s.priority ASC")
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
    @Query("SELECT s FROM Schedule s WHERE s.startTime IS NULL AND s.endTime IS NULL " +
            "ORDER BY s.isFeatured DESC, s.startDate ASC")
    List<Schedule> findAllDaySchedules();

    /**
     * 시간이 설정된 일정 조회
     * @return 시간이 설정된 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.startTime IS NOT NULL " +
            "ORDER BY s.isFeatured DESC, s.startDate ASC, s.startTime ASC")
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

    // === 새로 추가된 메서드들 ===

    /**
     * 추천 이벤트 조회
     * @return 추천 이벤트 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.isFeatured = true " +
            "ORDER BY s.startDate ASC, s.viewCount DESC")
    List<Schedule> findByIsFeaturedTrueOrderByStartDateAscViewCountDesc(Pageable pageable);

    /**
     * 추천 이벤트 개수 조회
     * @return 추천 이벤트 개수
     */
    long countByIsFeaturedTrue();

    /**
     * 조회수 기준 인기 일정 조회
     * @param minViewCount 최소 조회수
     * @param pageable 페이징 정보
     * @return 인기 일정 목록
     */
    List<Schedule> findByViewCountGreaterThanOrderByViewCountDesc(Integer minViewCount, Pageable pageable);

    /**
     * 제목과 카테고리로 검색
     * @param title 검색할 제목
     * @param category 카테고리
     * @return 검색된 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
            "AND s.category = :category " +
            "ORDER BY s.isFeatured DESC, s.startDate DESC")
    List<Schedule> findByTitleContainingIgnoreCaseAndCategoryOrderByStartDateDesc(
            @Param("title") String title, @Param("category") String category);

    /**
     * 카테고리별 일정 조회
     * @param category 카테고리
     * @return 해당 카테고리의 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.category = :category " +
            "ORDER BY s.isFeatured DESC, s.startDate DESC")
    List<Schedule> findByCategoryOrderByStartDateDesc(@Param("category") String category);

    /**
     * 전체 일정을 날짜 역순으로 조회 (검색 기본값용)
     * @return 전체 일정 목록
     */
    @Query("SELECT s FROM Schedule s ORDER BY s.isFeatured DESC, s.startDate DESC")
    List<Schedule> findAllByOrderByStartDateDesc();

    /**
     * 이미지가 있는 일정 조회
     * @return 이미지가 있는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.images IS NOT EMPTY " +
            "ORDER BY s.isFeatured DESC, s.startDate DESC")
    List<Schedule> findSchedulesWithImages();

    /**
     * 링크가 있는 일정 조회
     * @return 링크가 있는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.links IS NOT EMPTY " +
            "ORDER BY s.isFeatured DESC, s.startDate DESC")
    List<Schedule> findSchedulesWithLinks();

    /**
     * 특정 기간의 일정 개수 조회 (통계용)
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 기간의 일정 개수
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE " +
            "s.startDate >= :startDate AND s.startDate <= :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 전체 조회수 합계 조회
     * @return 전체 조회수 합계
     */
    @Query("SELECT SUM(s.viewCount) FROM Schedule s")
    Long sumAllViewCounts();

    /**
     * 최고 조회수 일정 조회
     * @param limit 조회할 개수
     * @return 최고 조회수 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.viewCount > 0 " +
            "ORDER BY s.viewCount DESC, s.startDate DESC")
    List<Schedule> findTopViewedSchedules(Pageable pageable);

    /**
     * 특정 우선순위와 추천 상태의 일정 조회
     * @param priority 우선순위
     * @param isFeatured 추천 여부
     * @return 해당 조건의 일정 목록
     */
    List<Schedule> findByPriorityAndIsFeaturedOrderByStartDateAsc(Integer priority, Boolean isFeatured);

    /**
     * 특정 날짜 이후의 추천 이벤트 조회
     * @param date 기준 날짜
     * @return 다가오는 추천 이벤트 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.isFeatured = true AND s.startDate >= :date " +
            "ORDER BY s.startDate ASC, s.priority ASC")
    List<Schedule> findUpcomingFeaturedSchedules(@Param("date") LocalDate date);

    /**
     * 특정 월의 추천 이벤트 조회
     * @param year 연도
     * @param month 월
     * @return 해당 월의 추천 이벤트 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.isFeatured = true AND " +
            "(YEAR(s.startDate) = :year AND MONTH(s.startDate) = :month " +
            "OR YEAR(s.endDate) = :year AND MONTH(s.endDate) = :month) " +
            "ORDER BY s.startDate ASC")
    List<Schedule> findFeaturedSchedulesByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 제목, 설명에서 키워드 검색 (전문 검색)
     * @param keyword 검색 키워드
     * @return 검색된 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.isFeatured DESC, s.viewCount DESC, s.startDate DESC")
    List<Schedule> findByKeywordInTitleOrDescription(@Param("keyword") String keyword);

    /**
     * 특정 기간 동안 생성된 일정 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 기간에 생성된 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE " +
            "DATE(s.createdAt) >= :startDate AND DATE(s.createdAt) <= :endDate " +
            "ORDER BY s.createdAt DESC")
    List<Schedule> findByCreatedAtBetween(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * 조회수가 특정 값 이상인 일정 개수
     * @param minViewCount 최소 조회수
     * @return 해당 조건의 일정 개수
     */
    long countByViewCountGreaterThanEqual(Integer minViewCount);

    /**
     * 카테고리별 일정 개수 통계
     * @return 카테고리별 일정 개수
     */
    @Query("SELECT s.category, COUNT(s) FROM Schedule s " +
            "WHERE s.category IS NOT NULL " +
            "GROUP BY s.category ORDER BY COUNT(s) DESC")
    List<Object[]> countByCategory();

    /**
     * 월별 추천 이벤트 개수 통계
     * @return 월별 추천 이벤트 개수
     */
    @Query("SELECT YEAR(s.startDate), MONTH(s.startDate), COUNT(s) " +
            "FROM Schedule s WHERE s.isFeatured = true " +
            "GROUP BY YEAR(s.startDate), MONTH(s.startDate) " +
            "ORDER BY YEAR(s.startDate) DESC, MONTH(s.startDate) DESC")
    List<Object[]> countFeaturedByMonth();

    /**
     * 사용자 정의 정렬을 위한 동적 쿼리용 기본 메서드
     * @param pageable 페이징 및 정렬 정보
     * @return 정렬된 일정 목록
     */
    @Query("SELECT s FROM Schedule s ORDER BY " +
            "CASE WHEN s.isFeatured = true THEN 0 ELSE 1 END, " +
            "s.startDate ASC, s.priority ASC")
    List<Schedule> findAllWithCustomSort(Pageable pageable);
}
