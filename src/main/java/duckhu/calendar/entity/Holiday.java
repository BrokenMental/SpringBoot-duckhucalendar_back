package duckhu.calendar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 공휴일/국경일 엔티티
 */
@Entity
@Table(name = "holidays", indexes = {
        @Index(name = "idx_holiday_date", columnList = "holidayDate"),
        @Index(name = "idx_holiday_country", columnList = "countryCode"),
        @Index(name = "idx_holiday_type", columnList = "holidayType")
})
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "공휴일 이름은 필수입니다")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "공휴일 날짜는 필수입니다")
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode = "KR"; // 기본값: 한국

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false)
    private HolidayType holidayType = HolidayType.PUBLIC;

    @Column(length = 500)
    private String description;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false; // 매년 반복 여부

    @Column(name = "color", length = 7)
    private String color = "#FF6B6B"; // 공휴일 표시 색상

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Holiday() {}

    // 생성자
    public Holiday(String name, LocalDate holidayDate, HolidayType holidayType) {
        this.name = name;
        this.holidayDate = holidayDate;
        this.holidayType = holidayType;
    }

    /**
     * 오늘이 이 공휴일인지 확인
     */
    public boolean isToday() {
        return LocalDate.now().equals(holidayDate);
    }

    // === 비즈니스 메서드 ===

    /**
     * 특정 연도의 공휴일인지 확인
     */
    public boolean isInYear(int year) {
        return holidayDate.getYear() == year;
    }

    /**
     * 짧은 이름 반환 (5자 이내)
     */
    public String getShortName() {
        if (name.length() <= 5) {
            return name;
        }
        return name.substring(0, 4) + "..";
    }

    public Long getId() {
        return id;
    }

    // === Getter and Setter ===

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public HolidayType getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(HolidayType holidayType) {
        this.holidayType = holidayType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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
        return "Holiday{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", holidayDate=" + holidayDate +
                ", holidayType=" + holidayType +
                ", countryCode='" + countryCode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Holiday holiday = (Holiday) o;
        return id != null && id.equals(holiday.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // 공휴일 타입 열거형
    public enum HolidayType {
        PUBLIC("공휴일"),           // 공휴일
        NATIONAL("국경일"),         // 국경일
        TRADITIONAL("전통 명절"),   // 전통 명절
        RELIGIOUS("종교 기념일"),   // 종교 기념일
        MEMORIAL("기념일"),         // 기념일
        SUBSTITUTE("대체공휴일");   // 대체공휴일

        private final String description;

        HolidayType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}