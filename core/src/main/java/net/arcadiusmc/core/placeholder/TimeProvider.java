package net.arcadiusmc.core.placeholder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import net.arcadiusmc.text.placeholder.ObjectPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import org.jetbrains.annotations.NotNull;

public enum TimeProvider implements ObjectPlaceholder<TimeProvider> {
  INSTANCE,
  ;

  @Override
  public Object lookup(@NotNull TimeProvider value, @NotNull String fieldName, @NotNull PlaceholderContext ctx) {
    return switch (fieldName) {
      case "date" -> LocalDate.now();
      case "time" -> LocalTime.now();

      default -> LocalDateTime.now();
    };
  }
}
