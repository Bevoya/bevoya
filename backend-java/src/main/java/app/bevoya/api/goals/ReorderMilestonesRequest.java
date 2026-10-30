package app.bevoya.api.goals;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** The goal's milestone ids in the wanted order. Must be exactly the goal's milestones. */
public record ReorderMilestonesRequest(@NotNull @Size(max = 200) List<@NotNull UUID> milestoneIds) {
}