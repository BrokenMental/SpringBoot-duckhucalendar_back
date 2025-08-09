package duckhu.calendar.entity;

import duckhu.calendar.enums.EventType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "제목은 필수입니다")
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull(message = "날짜는 필수입니다")
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(length = 1000)
    private String description;

    // 이미지 URL들 (JSON 배열로 저장)
    @Column(name = "images", columnDefinition = "JSON")
    private String images;

    // 링크들 (JSON 배열로 저장)
    @Column(name = "links", columnDefinition = "JSON")
    private String links;

    @Column(length = 7)
    private String color = "#4ECDC4";

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType = EventType.PUBLIC;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Event() {}

    // Getter와 Setter 메서드들
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getLinks() { return links; }
    public void setLinks(String links) { this.links = links; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
