package app.bevoya.api.sessions;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

record CompleteSessionRequest(
    @Min(1) @Max(5) Integer focusScore,
    @Size(max = 5000) String output,
    @Size(max = 10000) String notes) {

  static final CompleteSessionRequest EMPTY = new CompleteSessionRequest(null, null, null);
}