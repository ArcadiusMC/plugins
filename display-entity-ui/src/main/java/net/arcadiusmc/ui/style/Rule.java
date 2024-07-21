package net.arcadiusmc.ui.style;

import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.struct.Node;

@Getter
public class Rule<T> {

  private final T defaultValue;
  private final Class<T> type;
  private final boolean cascading;
  private final boolean layoutAffecting;
  private final boolean contentAffecting;
  private final StyleFunction<T> applicator;

  int id;
  String key;

  @Builder
  public Rule(
      Class<T> type,
      T defaultValue,
      boolean layoutAffecting,
      boolean contentAffecting,
      boolean cascading,
      StyleFunction<T> function
  ) {
    Objects.requireNonNull(type, "Null type");
    Objects.requireNonNull(defaultValue, "Null default value");

    this.type = type;
    this.defaultValue = defaultValue;
    this.cascading = cascading;
    this.applicator = function;
    this.layoutAffecting = layoutAffecting;
    this.contentAffecting = contentAffecting;

    this.id = -1;
    this.key = null;
  }

  public static <T> RuleBuilder<T> builder(Class<T> type) {
    RuleBuilder<T> builder = new RuleBuilder<>();
    builder.type = type;
    return builder;
  }

  public interface StyleFunction<T> {
    void apply(Node n, Screen screen, T t);
  }
}
