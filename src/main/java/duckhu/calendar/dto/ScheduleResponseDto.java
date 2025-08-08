package duckhu.calendar.dto;

import duckhu.calendar.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 향상된 일정 응답 DTO
 * 이미지, 링크, 추천 기능 등을 포함한 일정 데이터를 클라이언트에게 전송
 */
public class ScheduleResponseDto {

    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
    private Integer priority;
    private String color;
    private String category;

    // === 새로 추가된 필드들 ===
    private List<String> images;
    private List<String> links;
    private List<String> linkTitles;
    private Boolean isFeatured;
    private Integer viewCount;

    // === 기존 필드들 ===
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 기본 생성자
    public ScheduleResponseDto() {}

    // Entity에서 DTO로 변환하는 생성자
    public ScheduleResponseDto(Schedule schedule) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.startDate = schedule.getStartDate();
        this.endDate = schedule.getEndDate();
        this.startTime = schedule.getStartTime();
        this.endTime = schedule.getEndTime();
        this.description = schedule.getDescription();
        this.priority = schedule.getPriority();
        this.color = schedule.getColor();
        this.category = schedule.getCategory();

        // 새로 추가된 필드들
        this.images = schedule.getImages();
        this.links = schedule.getLinks();
        this.linkTitles = schedule.getLinkTitles();
        this.isFeatured = schedule.getIsFeatured();
        this.viewCount = schedule.getViewCount();

        this.createdAt = schedule.getCreatedAt();
        this.updatedAt = schedule.getUpdatedAt();
    }

    // 정적 팩토리 메서드
    public static ScheduleResponseDto from(Schedule schedule) {
        return new ScheduleResponseDto(schedule);
    }

    // === 비즈니스 로직 메서드들 ===

    /**
     * 종일 일정 여부 확인
     */
    public boolean isAllDay() {
        return startTime == null && endTime == null;
    }

    /**
     * 하루 일정 여부 확인
     */
    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }

    /**
     * 이미지가 있는지 확인
     */
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    /**
     * 링크가 있는지 확인
     */
    public boolean hasLinks() {
        return links != null && !links.isEmpty();
    }

    /**
     * 추천 이벤트 여부 확인
     */
    public boolean isFeaturedEvent() {
        return isFeatured != null && isFeatured;
    }

    /**
     * 이미지 개수 반환
     */
    public int getImageCount() {
        return images != null ? images.size() : 0;
    }

    /**
     * 링크 개수 반환
     */
    public int getLinkCount() {
        return links != null ? links.size() : 0;
    }

    /**
     * 우선순위 텍스트 반환
     */
    public String getPriorityText() {
        if (priority == null) return "중간";
        return switch (priority) {
            case 1 -> "높음";
            case 2 -> "중간";
            case 3 -> "낮음";
            default -> "중간";
        };
    }

    /**
     * 일정 기간 계산 (일 단위)
     */
    public long getDurationInDays() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 시작일까지 남은 일수 계산
     */
    public long getDaysUntilStart() {
        if (startDate == null) return 0;
        LocalDate today = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(today, startDate);
    }

    /**
     * 이벤트 상태 반환 (시작 전, 진행 중, 종료)
     */
    public String getEventStatus() {
        LocalDate today = LocalDate.now();

        if (startDate.isAfter(today)) {
            return "UPCOMING";
        } else if (endDate.isBefore(today)) {
            return "COMPLETED";
        } else {
            return "ONGOING";
        }
    }

    /**
     * 한국어 이벤트 상태 반환
     */
    public String getEventStatusKorean() {
        return switch (getEventStatus()) {
            case "UPCOMING" -> "예정";
            case "ONGOING" -> "진행 중";
            case "COMPLETED" -> "종료";
            default -> "알 수 없음";
        };
    }

    // === Getter and Setter ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public List<String> getLinkTitles() {
        return linkTitles;
    }

    public void setLinkTitles(List<String> linkTitles) {
        this.linkTitles = linkTitles;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ScheduleResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", priority=" + priority +
                ", isFeatured=" + isFeatured +
                ", viewCount=" + viewCount +
                ", imageCount=" + getImageCount() +
                ", linkCount=" + getLinkCount() +
                ", status='" + getEventStatus() + '\'' +
                '}';
    }
}
