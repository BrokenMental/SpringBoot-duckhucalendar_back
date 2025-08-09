package duckhu.calendar.repository;

import duckhu.calendar.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 공휴일 Repository
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * 특정 날짜의 공휴일 조회
     * @param date 조회할 날짜
     * @return 해당 날짜의 공휴일 목록
     */
    List<Holiday> findByHolidayDateOrderByHolidayTypeAsc(LocalDate date);

    /**
     * 특정 날짜 범위의 공휴일 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 범위의 공휴일 목록
     */
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 연도의 공휴일 조회
     * @param year 연도
     * @return 해당 연도의 공휴일 목록
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year ORDER BY h.holidayDate ASC")
    List<Holiday> findByYear(@Param("year") int year);

    /**
     * 특정 월의 공휴일 조회
     * @param year 연도
     * @param month 월
     * @return 해당 월의 공휴일 목록
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND MONTH(h.holidayDate) = :month ORDER BY h.holidayDate ASC")
    List<Holiday> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 국가별 공휴일 조회
     * @param countryCode 국가 코드
     * @param year 연도
     * @return 해당 국가의 공휴일 목록
     */
    @Query("SELECT h FROM Holiday h WHERE h.countryCode = :countryCode AND YEAR(h.holidayDate) = :year ORDER BY h.holidayDate ASC")
    List<Holiday> findByCountryCodeAndYear(@Param("countryCode") String countryCode, @Param("year") int year);

    /**
     * 공휴일 타입별 조회
     * @param holidayType 공휴일 타입
     * @param year 연도
     * @return 해당 타입의 공휴일 목록
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayType = :holidayType AND YEAR(h.holidayDate) = :year ORDER BY h.holidayDate ASC")
    List<Holiday> findByHolidayTypeAndYear(@Param("holidayType") Holiday.HolidayType holidayType, @Param("year") int year);

    /**
     * 오늘의 공휴일 조회
     * @return 오늘의 공휴일 목록
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate = CURRENT_DATE ORDER BY h.holidayType ASC")
    List<Holiday> findTodayHolidays();

    /**
     * 다가오는 공휴일 조회 (앞으로 N일간)
     * @param endDate 며칠 후까지
     * @return 다가오는 공휴일 목록
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate >= CURRENT_DATE AND h.holidayDate <= :endDate ORDER BY h.holidayDate ASC")
    List<Holiday> findUpcomingHolidays(@Param("endDate") LocalDate endDate);

    /**
     * 특정 날짜에 공휴일이 있는지 확인
     * @param date 확인할 날짜
     * @return 공휴일 존재 여부
     */
    boolean existsByHolidayDate(LocalDate date);

    /**
     * 특정 연도의 공휴일 개수
     * @param year 연도
     * @return 공휴일 개수
     */
    @Query("SELECT COUNT(h) FROM Holiday h WHERE YEAR(h.holidayDate) = :year")
    long countByYear(@Param("year") int year);

    /**
     * 중복 공휴일 확인 (같은 날짜, 같은 이름)
     * @param name 공휴일 이름
     * @param holidayDate 날짜
     * @return 존재 여부
     */
    boolean existsByNameAndHolidayDate(String name, LocalDate holidayDate);
}