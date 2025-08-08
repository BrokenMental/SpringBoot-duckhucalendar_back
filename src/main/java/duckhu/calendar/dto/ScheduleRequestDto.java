package duckhu.calendar.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 향상된 일정 요청 DTO
 * 이미지, 링크, 추천 기능 등을 포함한 일정 데이터를 받기 위한 클래스
 */
public class ScheduleRequestDto {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다")
    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 1000, message = "상세 내용은 1000자를 초과할 수 없습니다")
    private String description;

    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
    @Max(value = 3, message = "우선순위는 3 이하여야 합니다")
    private Integer priority = 2;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "올바른 색상 코드 형식이 아닙니다")
    private String color;

    /**
     * 카테고리 (검색 및 분류용)
     */
    @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
    private String category;

    // === 새로 추가된 필드들 ===

    /**
     * 이미지 URL 목록 (최대 3개)
     */
    @Size(max = 3, message = "이미지는 최대 3개까지 업로드할 수 있습니다")
    private List<@NotBlank @Size(max = 500) String> images;

    /**
     * 링크 URL 목록 (최대 2개)
     */
    @Size(max = 2, message = "링크는 최대 2개까지 추가할 수 있습니다")
    private List<@NotBlank @Size(max = 500) String> links;

    /**
     * 링크 제목 목록 (links와 동일한 크기여야 함)
     */
    private List<@Size(max = 100) String> linkTitles;

    /**
     * 추천 이벤트 여부 (관리자만 설정 가능)
     */
    private Boolean isFeatured = false;

    // 기본 생성자
    public ScheduleRequestDto() {}

    // 생성자
    public ScheduleRequestDto(String title, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // === 유효성 검사 메서드들 ===

    /**
     * 날짜 유효성 검사
     */
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * 시간 유효성 검사
     */
    public boolean isValidTimeRange() {
        // 시간이 둘 다 있는 경우에만 검증
        if (startTime != null && endTime != null) {
            // 같은 날짜인 경우에만 시간 순서 확인
            if (startDate.equals(endDate)) {
                return !startTime.isAfter(endTime);
            }
        }
        return true;
    }

    /**
     * 이미지 URL 유효성 검사
     */
    public boolean areImagesValid() {
        if (images == null || images.isEmpty()) {
            return true; // 이미지는 선택사항
        }

        // 최대 3개 제한
        if (images.size() > 3) {
            return false;
        }

        // 각 URL 유효성 검사
        return images.stream().allMatch(this::isValidUrl);
    }

    /**
     * 링크 유효성 검사
     */
    public boolean areLinksValid() {
        if (links == null || links.isEmpty()) {
            return true; // 링크는 선택사항
        }

        // 최대 2개 제한
        if (links.size() > 2) {
            return false;
        }

        // 링크 제목과 개수가 일치하는지 확인
        if (linkTitles != null && linkTitles.size() != links.size()) {
            return false;
        }

        // 각 URL 유효성 검사
        return links.stream().allMatch(this::isValidUrl);
    }

    /**
     * URL 형식 검증
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            new java.net.URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 전체 유효성 검사
     */
    public boolean isValid() {
        return isValidDateRange() &&
                isValidTimeRange() &&
                areImagesValid() &&
                areLinksValid();
    }

    // === 헬퍼 메서드들 ===

    /**
     * 링크 제목 자동 생성 (제목이 없는 경우)
     */
    public void generateLinkTitlesIfMissing() {
        if (links != null && !links.isEmpty()) {
            if (linkTitles == null) {
                linkTitles = new java.util.ArrayList<>();
            }

            while (linkTitles.size() < links.size()) {
                linkTitles.add("링크 " + (linkTitles.size() + 1));
            }
        }
    }

    /**
     * 빈 리스트들을 null로 정리
     */
    public void cleanEmptyLists() {
        if (images != null && images.isEmpty()) {
            images = null;
        }
        if (links != null && links.isEmpty()) {
            links = null;
        }
        if (linkTitles != null && linkTitles.isEmpty()) {
            linkTitles = null;
        }
    }

    // === Getter and Setter ===

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

    @Override
    public String toString() {
        return "ScheduleRequestDto{" +
                "title='" + title + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", priority=" + priority +
                ", isFeatured=" + isFeatured +
                ", imagesCount=" + (images != null ? images.size() : 0) +
                ", linksCount=" + (links != null ? links.size() : 0) +
                '}';
    }
}
