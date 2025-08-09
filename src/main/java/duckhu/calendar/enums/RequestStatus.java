package duckhu.calendar.enums;

/**
 * 요청 상태 열거형
 */
public enum RequestStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("거절됨"),
    PROCESSING("처리중");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
