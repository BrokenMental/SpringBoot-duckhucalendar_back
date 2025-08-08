package duckhu.calendar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 향상된 일정 엔티티
 * 이미지, 링크, 추천 기능 등이 추가된 스케줄 정보를 저장
 */
@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_schedule_date_range", columnList = "startDate, endDate"),
        @Index(name = "idx_schedule_priority", columnList = "priority"),
        @Index(name = "idx_schedule_featured", columnList = "isFeatured, startDate"),
        @Index(name = "idx_schedule_view_count", columnList = "viewCount")
})
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull(message = "시작일은 필수입니다")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Size(max = 1000, message = "상세 내용은 1000자를 초과할 수 없습니다")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer priority = 2; // 기본값: 중간 우선순위

    @Size(max = 7, message = "색상 코드는 7자를 초과할 수 없습니다")
    @Column(length = 7, nullable = false)
    private String color = "#007bff"; // 기본값: 파란색

    /**
     * 카테고리 (검색 및 분류용)
     */
    @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
    @Column(length = 50)
    private String category;

    // === 새로 추가된 필드들 ===

    /**
     * 이미지 URL 배열 (최대 3개)
     * PostgreSQL의 TEXT[] 타입 사용
     */
    @ElementCollection
    @CollectionTable(name = "schedule_images", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    @Size(max = 3, message = "이미지는 최대 3개까지 업로드할 수 있습니다")
    private List<String> images;

    /**
     * 링크 URL 배열 (최대 2개)
     */
    @ElementCollection
    @CollectionTable(name = "schedule_links", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "link_url", columnDefinition = "TEXT")
    @Size(max = 2, message = "링크는 최대 2개까지 추가할 수 있습니다")
    private List<String> links;

    /**
     * 링크 제목 배열 (links와 동일한 크기)
     */
    @ElementCollection
    @CollectionTable(name = "schedule_link_titles", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "link_title", length = 100)
    private List<String> linkTitles;

    /**
     * 추천 이벤트 여부
     */
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    /**
     * 조회수
     */
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    // === 기존 필드들 ===

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Schedule() {}

    // 생성자
    public Schedule(String title, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
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
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    /**
     * 추천 상태 토글
     */
    public void toggleFeatured() {
        this.isFeatured = !this.isFeatured;
    }

    /**
     * 이미지 추가
     * @param imageUrl 이미지 URL
     * @return 추가 성공 여부
     */
    public boolean addImage(String imageUrl) {
        if (images == null) {
            images = new java.util.ArrayList<>();
        }
        if (images.size() >= 3) {
            return false; // 최대 3개 제한
        }
        images.add(imageUrl);
        return true;
    }

    /**
     * 링크 추가
     * @param linkUrl 링크 URL
     * @param linkTitle 링크 제목
     * @return 추가 성공 여부
     */
    public boolean addLink(String linkUrl, String linkTitle) {
        if (links == null) {
            links = new java.util.ArrayList<>();
            linkTitles = new java.util.ArrayList<>();
        }
        if (links.size() >= 2) {
            return false; // 최대 2개 제한
        }
        links.add(linkUrl);
        linkTitles.add(linkTitle != null ? linkTitle : "링크 " + (links.size()));
        return true;
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
        return "Schedule{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", priority=" + priority +
                ", isFeatured=" + isFeatured +
                ", viewCount=" + viewCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return id != null && id.equals(schedule.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
