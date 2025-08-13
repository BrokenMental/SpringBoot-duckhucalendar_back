package duckhu.calendar.repository;

import duckhu.calendar.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 활성화된 공지사항 조회 (표시용)
     * 우선순위 높은 순, 최신 순으로 정렬
     */
    @Query("SELECT n FROM Notice n " +
            "WHERE n.isActive = true " +
            "AND (n.startDate IS NULL OR n.startDate <= :now) " +
            "AND (n.endDate IS NULL OR n.endDate >= :now) " +
            "ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notice> findActiveNoticesForDisplay(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 모든 공지사항 조회 (관리자용)
     * 우선순위 높은 순, 최신 순으로 정렬
     */
    List<Notice> findAllByOrderByPriorityDescCreatedAtDesc();

    /**
     * 활성화된 공지사항만 조회
     */
    List<Notice> findByIsActiveTrueOrderByPriorityDescCreatedAtDesc();

    /**
     * 특정 날짜 범위의 공지사항 조회
     */
    @Query("SELECT n FROM Notice n " +
            "WHERE n.isActive = true " +
            "AND n.startDate >= :startDate " +
            "AND (n.endDate IS NULL OR n.endDate <= :endDate) " +
            "ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notice> findNoticesByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}
