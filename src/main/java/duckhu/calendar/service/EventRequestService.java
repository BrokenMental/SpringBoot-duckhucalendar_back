package duckhu.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import duckhu.calendar.dto.ScheduleRequestDto;
import duckhu.calendar.entity.EventRequest;
import duckhu.calendar.enums.RequestStatus;
import duckhu.calendar.repository.EventRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventRequestService {

    @Autowired
    private EventRequestRepository eventRequestRepository;

    @Autowired
    private EmailService emailService;

    private final ObjectMapper objectMapper;
    // 이메일 인증 코드 임시 저장 (실제로는 Redis 사용 권장)
    private final ConcurrentHashMap<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    @Autowired
    private ScheduleService scheduleService;

    public EventRequestService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 이벤트 요청 제출
     */
    @Transactional
    public EventRequest submitRequest(EventRequest eventRequest) {
        eventRequest.setStatus(RequestStatus.PENDING);
        EventRequest saved = eventRequestRepository.save(eventRequest);

        // 관리자에게 알림 이메일 발송 (선택사항)
        notifyAdminsAboutNewRequest(saved);

        return saved;
    }

    /**
     * 이메일 인증 코드 전송
     */
    public void sendVerificationCode(String email) {
        String code = emailService.sendVerificationCode(email);

        // 인증 코드를 5분간 저장
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);
        verificationCodes.put(email, new VerificationData(code, expiryTime));

        // 만료된 코드들 정리
        cleanExpiredCodes();
    }

    /**
     * 요청 상태 업데이트 (관리자용)
     */
    @Transactional
    public void updateRequestStatus(Long requestId, String statusStr) {
        EventRequest request = eventRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        RequestStatus newStatus = RequestStatus.valueOf(statusStr.toUpperCase());
        request.setStatus(newStatus);
        eventRequestRepository.save(request);

        // 승인된 경우 실제 이벤트 생성
        if (newStatus == RequestStatus.APPROVED && "ADD".equals(request.getRequestType())) {
            createScheduleFromRequest(request);
        }

        // 요청자에게 결과 이메일 발송
        sendStatusUpdateEmail(request);
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
     * 요청으로부터 일정 생성
     */
    private void createScheduleFromRequest(EventRequest request) {
        try {
            // JSON 문자열을 ScheduleRequestDto로 변환
            ScheduleRequestDto scheduleDto = objectMapper.readValue(
                    request.getEventData(),
                    ScheduleRequestDto.class
            );

            // 일정 생성
            scheduleService.createSchedule(scheduleDto);

        } catch (Exception e) {
            throw new RuntimeException("일정 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 만료된 인증 코드 정리
     */
    private void cleanExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodes.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().expiryTime)
        );
    }

    /**
     * 관리자에게 새 요청 알림
     */
    private void notifyAdminsAboutNewRequest(EventRequest request) {
        // TODO: 관리자 이메일 목록 가져와서 알림 발송
        System.out.println("새 이벤트 요청: " + request.getId());
    }

    /**
     * 요청 상태 변경 이메일 발송
     */
    private void sendStatusUpdateEmail(EventRequest request) {
        String subject = "이벤트 요청 처리 결과";
        String body = String.format(
                "안녕하세요,\n\n귀하의 이벤트 요청이 %s되었습니다.\n\n감사합니다.",
                request.getStatus() == RequestStatus.APPROVED ? "승인" : "거절"
        );

        emailService.sendEmail(request.getRequesterEmail(), subject, body);
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
