package duckhu.calendar.enums;

/**
 * 이벤트 타입 열거형
 */
public enum EventType {
    PUBLIC("공개 이벤트"),
    PRIVATE("비공개 이벤트"),
    HOLIDAY("공휴일"),
    ANNIVERSARY("기념일"),
    MEETING("회의"),
    DEADLINE("마감일");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
