package duckhu.calendar.repository;

import duckhu.calendar.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 공휴일/국경일 리포지토리
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * 날짜 범위별 공휴일 조회 (날짜순, 타입순 정렬)
     */
    List<Holiday> findByHolidayDateBetweenAndCountryCodeOrderByHolidayDateAscHolidayTypeAsc(
            LocalDate startDate, LocalDate endDate, String countryCode);

    /**
     * 특정 날짜의 공휴일 조회 (타입순 정렬 - 국경일 우선)
     */
    List<Holiday> findByHolidayDateAndCountryCodeOrderByHolidayTypeAsc(
            LocalDate holidayDate, String countryCode);

    /**
     * 연도별 공휴일 조회
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND h.countryCode = :countryCode ORDER BY h.holidayDate ASC, h.holidayType ASC")
    List<Holiday> findByYearAndCountryCode(@Param("year") int year, @Param("countryCode") String countryCode);

    /**
     * 월별 공휴일 조회
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND MONTH(h.holidayDate) = :month AND h.countryCode = :countryCode ORDER BY h.holidayDate ASC, h.holidayType ASC")
    List<Holiday> findByYearAndMonthAndCountryCode(
            @Param("year") int year,
            @Param("month") int month,
            @Param("countryCode") String countryCode);

    /**
     * 공휴일 타입별 조회
     */
    List<Holiday> findByHolidayTypeAndCountryCodeOrderByHolidayDateAsc(
            Holiday.HolidayType holidayType, String countryCode);

    /**
     * 반복 공휴일 조회
     */
    List<Holiday> findByIsRecurringTrueAndCountryCodeOrderByHolidayDateAsc(String countryCode);

    /**
     * 공휴일명으로 검색
     */
    List<Holiday> findByNameContainingIgnoreCaseAndCountryCodeOrderByHolidayDateAsc(
            String name, String countryCode);

    /**
     * 특정 날짜 범위의 공휴일 개수 조회
     */
    long countByHolidayDateBetweenAndCountryCode(
            LocalDate startDate, LocalDate endDate, String countryCode);

    /**
     * 오늘 이후의 다음 공휴일 조회
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate >= :today AND h.countryCode = :countryCode ORDER BY h.holidayDate ASC LIMIT 1")
    Holiday findNextHoliday(@Param("today") LocalDate today, @Param("countryCode") String countryCode);

    /**
     * 특정 년도의 국경일만 조회
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND h.countryCode = :countryCode AND h.holidayType = 'NATIONAL' ORDER BY h.holidayDate ASC")
    List<Holiday> findNationalHolidaysByYear(@Param("year") int year, @Param("countryCode") String countryCode);

    /**
     * 특정 년도의 공휴일만 조회 (국경일 제외)
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND h.countryCode = :countryCode AND h.holidayType = 'PUBLIC' ORDER BY h.holidayDate ASC")
    List<Holiday> findPublicHolidaysByYear(@Param("year") int year, @Param("countryCode") String countryCode);

    /**
     * 이번 주 공휴일 조회
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :weekStart AND :weekEnd AND h.countryCode = :countryCode ORDER BY h.holidayDate ASC, h.holidayType ASC")
    List<Holiday> findHolidaysInWeek(
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            @Param("countryCode") String countryCode);

    /**
     * 이름, 날짜, 국가로 중복 확인
     */
    boolean existsByNameAndHolidayDateAndCountryCode(String name, LocalDate holidayDate, String countryCode);

    /**
     * 이름과 날짜로 중복 확인 (기존 메서드와 호환성 유지)
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Holiday h WHERE h.name = :name AND h.holidayDate = :holidayDate")
    boolean existsByNameAndHolidayDate(@Param("name") String name, @Param("holidayDate") LocalDate holidayDate);

    /**
     * 특정 월의 공휴일 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND MONTH(h.holidayDate) = :month AND h.countryCode = :countryCode")
    boolean existsHolidayInMonth(@Param("year") int year, @Param("month") int month, @Param("countryCode") String countryCode);
}
