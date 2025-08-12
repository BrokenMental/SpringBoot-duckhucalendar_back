package duckhu.calendar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import duckhu.calendar.entity.Holiday;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 공휴일/국경일 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDTO {

    private Long id;

    @NotBlank(message = "공휴일 이름은 필수입니다")
    private String name;

    @NotNull(message = "공휴일 날짜는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    private String countryCode = "KR";

    private Holiday.HolidayType holidayType = Holiday.HolidayType.PUBLIC;

    private String description;

    private Boolean isRecurring = false;

    private String color = "#FF6B6B";

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static HolidayDTO fromEntity(Holiday holiday) {
        return HolidayDTO.builder()
                .id(holiday.getId())
                .name(holiday.getName())
                .holidayDate(holiday.getHolidayDate())
                .countryCode(holiday.getCountryCode())
                .holidayType(holiday.getHolidayType())
                .description(holiday.getDescription())
                .isRecurring(holiday.getIsRecurring())
                .color(holiday.getColor())
                .createdAt(holiday.getCreatedAt())
                .updatedAt(holiday.getUpdatedAt())
                .build();
    }

    /**
     * DTO -> Entity 변환 (새 엔티티 생성용)
     */
    public Holiday toEntity() {
        Holiday holiday = new Holiday();
        holiday.setName(this.name);
        holiday.setHolidayDate(this.holidayDate);
        holiday.setCountryCode(this.countryCode != null ? this.countryCode : "KR");
        holiday.setHolidayType(this.holidayType != null ? this.holidayType : Holiday.HolidayType.PUBLIC);
        holiday.setDescription(this.description);
        holiday.setIsRecurring(this.isRecurring != null ? this.isRecurring : false);
        holiday.setColor(this.color != null ? this.color : "#FF6B6B");
        return holiday;
    }

    /**
     * 기존 Entity 업데이트 (ID가 있는 경우)
     */
    public void updateEntity(Holiday holiday) {
        if (this.name != null) holiday.setName(this.name);
        if (this.holidayDate != null) holiday.setHolidayDate(this.holidayDate);
        if (this.countryCode != null) holiday.setCountryCode(this.countryCode);
        if (this.holidayType != null) holiday.setHolidayType(this.holidayType);
        if (this.description != null) holiday.setDescription(this.description);
        if (this.isRecurring != null) holiday.setIsRecurring(this.isRecurring);
        if (this.color != null) holiday.setColor(this.color);
    }

    /**
     * 공휴일 타입별 우선순위 반환 (국경일이 높은 우선순위)
     */
    public int getPriority() {
        if (holidayType == null) {
            return 999;
        }

        switch (holidayType) {
            case NATIONAL:
                return 1; // 국경일이 가장 높은 우선순위
            case PUBLIC:
                return 2; // 공휴일
            case SUBSTITUTE:
                return 3; // 대체공휴일
            case MEMORIAL:
                return 4; // 기념일
            case ANNIVERSARY:
                return 5; // 기타 기념일
            default:
                return 999;
        }
    }

    /**
     * 공휴일 타입별 한글 이름 반환
     */
    public String getHolidayTypeName() {
        if (holidayType == null) {
            return "기타";
        }

        switch (holidayType) {
            case NATIONAL:
                return "국경일";
            case PUBLIC:
                return "공휴일";
            case SUBSTITUTE:
                return "대체공휴일";
            case MEMORIAL:
                return "기념일";
            case ANNIVERSARY:
                return "기타";
            default:
                return "기타";
        }
    }

    /**
     * 유효성 검증
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
                holidayDate != null &&
                countryCode != null && !countryCode.trim().isEmpty();
    }

    /**
     * 날짜 문자열로 반환 (프론트엔드용)
     */
    public String getHolidayDateString() {
        return holidayDate != null ? holidayDate.toString() : null;
    }

    /**
     * 월-일 형식으로 반환
     */
    public String getMonthDay() {
        if (holidayDate == null) return "";
        return String.format("%02d-%02d", holidayDate.getMonthValue(), holidayDate.getDayOfMonth());
    }

    /**
     * 요일 반환
     */
    public String getDayOfWeek() {
        if (holidayDate == null) return "";

        String[] dayNames = {"일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"};
        return dayNames[holidayDate.getDayOfWeek().getValue() % 7];
    }
}
