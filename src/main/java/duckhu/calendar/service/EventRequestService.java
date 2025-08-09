package duckhu.calendar.service;

import duckhu.calendar.entity.EventRequest;
import duckhu.calendar.enums.RequestStatus;
import duckhu.calendar.repository.EventRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventRequestService {

    // 이메일 인증 코드 임시 저장 (실제로는 Redis 사용 권장)
    private final ConcurrentHashMap<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    @Autowired
    private EventRequestRepository eventRequestRepository;
    @Autowired
    private EmailService emailService;

    /**
     * 이벤트 요청 제출
     */
    public EventRequest submitRequest(EventRequest eventRequest) {
        eventRequest.setStatus(RequestStatus.PENDING);
        return eventRequestRepository.save(eventRequest);
    }

    /**
     * 이메일 인증 코드 전송
     */
    public void sendVerificationCode(String email) {
        String code = emailService.sendVerificationCode(email);

        // 인증 코드를 5분간 저장
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);
        verificationCodes.put(email, new VerificationData(code, expiryTime));

        // 만료된 코드들 정리 (옵션)
        cleanExpiredCodes();
    }

    /**
     * 이메일 인증 확인
     */
    public boolean verifyEmail(String email, String inputCode) {
        VerificationData data = verificationCodes.get(email);

        if (data == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(data.expiryTime)) {
            verificationCodes.remove(email);
            return false;
        }

        if (data.code.equals(inputCode)) {
            verificationCodes.remove(email); // 인증 성공 시 제거
            return true;
        }

        return false;
    }

    /**
     * 모든 요청 조회 (관리자용)
     */
    public List<EventRequest> getAllRequests() {
        return eventRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 요청 상태 업데이트 (관리자용)
     */
    public void updateRequestStatus(Long requestId, String statusStr) {
        EventRequest request = eventRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        RequestStatus status = RequestStatus.valueOf(statusStr.toUpperCase());
        request.setStatus(status);
        eventRequestRepository.save(request);
    }

    /**
     * 만료된 인증 코드 정리
     */
    private void cleanExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodes.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().expiryTime));
    }

    /**
     * 인증 데이터 클래스
     */
    private static class VerificationData {
        String code;
        LocalDateTime expiryTime;

        VerificationData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}
