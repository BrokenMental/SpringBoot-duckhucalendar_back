package duckhu.calendar.exception;

/**
 * 공공데이터 API 예외 클래스
 */
public class PublicDataApiException extends RuntimeException {
    private final int statusCode;

    public PublicDataApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public PublicDataApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
