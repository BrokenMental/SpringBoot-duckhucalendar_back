package duckhu.calendar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 모든 Controller에서 발생하는 예외를 일관되게 처리
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 일반적인 RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * 유효성 검사 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", "입력값 검증 실패");
        response.put("fieldErrors", errors);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 권한 없음 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
        return createErrorResponse("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    /**
     * 엔티티를 찾을 수 없을 때
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * 이메일 발송 실패
     */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<?> handleEmailSendException(EmailSendException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * 중복 데이터 예외
     */
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<?> handleDuplicateException(DuplicateException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        e.printStackTrace(); // 개발 중에는 스택트레이스 출력
        return createErrorResponse("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 통일된 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());

        return ResponseEntity.status(status).body(response);
    }
}
