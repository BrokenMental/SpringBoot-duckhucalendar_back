package duckhu.calendar.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 요청 DTO
 * Vue 프론트엔드에서 전송되는 일정 데이터를 받기 위한 클래스
 */
public class ScheduleRequestDto {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
    private String title;

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다")
    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 500, message = "상세 내용은 500자를 초과할 수 없습니다")
    private String description;

    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
    @Max(value = 3, message = "우선순위는 3 이하여야 합니다")
    private Integer priority = 2;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "올바른 색상 코드 형식이 아닙니다")
    private String color;

    // 기본 생성자
    public ScheduleRequestDto() {}

    // 생성자
    public ScheduleRequestDto(String title, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 날짜 유효성 검사 (커스텀 검증)
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    // 시간 유효성 검사
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

    // Getter and Setter
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

    @Override
    public String toString() {
        return "ScheduleRequestDto{" +
                "title='" + title + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", priority=" + priority +
                '}';
    }
}
