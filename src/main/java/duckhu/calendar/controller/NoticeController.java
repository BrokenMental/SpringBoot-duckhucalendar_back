package duckhu.calendar.controller;

import duckhu.calendar.entity.Notice;
import duckhu.calendar.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 활성화된 공지사항 목록 조회 (사용자용)
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveNotices(
            @RequestParam(defaultValue = "2") int limit) {
        try {
            List<Notice> notices = noticeService.getActiveNotices(limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notices", notices,
                    "message", "공지사항을 성공적으로 조회했습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "공지사항 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 모든 공지사항 목록 조회 (관리자용)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllNotices() {
        try {
            List<Notice> notices = noticeService.getAllNotices();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notices", notices,
                    "message", "모든 공지사항을 성공적으로 조회했습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "공지사항 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 공지사항 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNoticeById(@PathVariable Long id) {
        try {
            Notice notice = noticeService.getNoticeById(id)
                    .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notice", notice,
                    "message", "공지사항을 성공적으로 조회했습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 공지사항 생성 (관리자용)
     */
    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> createNotice(@RequestBody Notice notice) {
        try {
            Notice createdNotice = noticeService.createNotice(notice);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notice", createdNotice,
                    "message", "공지사항이 성공적으로 생성되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "공지사항 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 공지사항 수정 (관리자용)
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> updateNotice(@PathVariable Long id, @RequestBody Notice notice) {
        try {
            Notice updatedNotice = noticeService.updateNotice(id, notice);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notice", updatedNotice,
                    "message", "공지사항이 성공적으로 수정되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "공지사항 수정에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 공지사항 삭제 (관리자용)
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotice(@PathVariable Long id) {
        try {
            noticeService.deleteNotice(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "공지사항이 성공적으로 삭제되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "공지사항 삭제에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 공지사항 활성화/비활성화 토글 (관리자용)
     */
    @PatchMapping("/admin/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleNoticeStatus(@PathVariable Long id) {
        try {
            Notice updatedNotice = noticeService.toggleNoticeStatus(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notice", updatedNotice,
                    "message", "공지사항 상태가 성공적으로 변경되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "공지사항 상태 변경에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}
