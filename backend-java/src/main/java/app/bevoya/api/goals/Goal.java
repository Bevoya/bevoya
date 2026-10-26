package app.bevoya.api.goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goals", schema = "public")
class Goal {

  static final String STATUS_PATTERN = "active|paused|completed|archived";

  // Generated in Java and null until persist, so Spring Data treats save() as an insert
  // (no extra SELECT, which would cost a round trip to Mumbai).
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false)
  private String title;

  private String description;

  @Column(nullable = false)
  private String status;

  @Column(name = "target_date")
  private LocalDate targetDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Goal() {
    // for JPA
  }

  static Goal create(UUID userId, String title, String description, String status,
      LocalDate targetDate, Instant now) {
    Goal goal = new Goal();
    goal.userId = userId;
    goal.title = title;
    goal.description = description;
    goal.status = status;
    goal.targetDate = targetDate;
    goal.createdAt = now;
    goal.updatedAt = now;
    return goal;
  }

  void replace(String title, String description, String status, LocalDate targetDate, Instant now) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.targetDate = targetDate;
    this.updatedAt = now; // no database trigger sets this
  }

  UUID getId() { return id; }
  String getTitle() { return title; }
  String getDescription() { return description; }
  String getStatus() { return status; }
  LocalDate getTargetDate() { return targetDate; }
  Instant getCreatedAt() { return createdAt; }
  Instant getUpdatedAt() { return updatedAt; }
}