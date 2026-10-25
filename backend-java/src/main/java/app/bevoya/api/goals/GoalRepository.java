package app.bevoya.api.goals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Plain Repository, not JpaRepository: findById, findAll and deleteById do not exist,
 * so nobody can reach another user's goal by accident. Every read and delete names the owner.
 */
interface GoalRepository extends Repository<Goal, UUID> {

  Goal save(Goal goal);

  Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

  List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId, Limit limit);

  List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status, Limit limit);

  /** Returns the number of rows deleted: 1 if it was yours, 0 otherwise. One round trip. */
  @Modifying
  @Query("delete from Goal g where g.id = :id and g.userId = :userId")
  int deleteOwned(@Param("id") UUID id, @Param("userId") UUID userId);
}