package duckhu.calendar.service;

import duckhu.calendar.entity.Notice;
import duckhu.calendar.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    /**
     * 활성화된 공지사항 목록 조회 (사용자용)
     * @param limit 조회할 개수
     */
    public List<Notice> getActiveNotices(int limit) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);

        return noticeRepository.findActiveNoticesForDisplay(now, pageable);
    }

    /**
     * 모든 공지사항 목록 조회 (관리자용)
     */
    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByPriorityDescCreatedAtDesc();
    }

    /**
     * 공지사항 상세 조회
     */
    public Optional<Notice> getNoticeById(Long id) {
        return noticeRepository.findById(id);
    }

    /**
     * 공지사항 생성
     */
    @Transactional
    public Notice createNotice(Notice notice) {
        // 시작일이 없으면 현재 시간으로 설정
        if (notice.getStartDate() == null) {
            notice.setStartDate(LocalDateTime.now());
        }

        return noticeRepository.save(notice);
    }

    /**
     * 공지사항 수정
     */
    @Transactional
    public Notice updateNotice(Long id, Notice updatedNotice) {
        Notice existingNotice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        existingNotice.setTitle(updatedNotice.getTitle());
        existingNotice.setContent(updatedNotice.getContent());
        existingNotice.setIsActive(updatedNotice.getIsActive());
        existingNotice.setPriority(updatedNotice.getPriority());
        existingNotice.setStartDate(updatedNotice.getStartDate());
        existingNotice.setEndDate(updatedNotice.getEndDate());

        return noticeRepository.save(existingNotice);
    }

    /**
     * 공지사항 삭제
     */
    @Transactional
    public void deleteNotice(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new RuntimeException("공지사항을 찾을 수 없습니다.");
        }
        noticeRepository.deleteById(id);
    }

    /**
     * 공지사항 활성화/비활성화 토글
     */
    @Transactional
    public Notice toggleNoticeStatus(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        notice.setIsActive(!notice.getIsActive());
        return noticeRepository.save(notice);
    }
}
