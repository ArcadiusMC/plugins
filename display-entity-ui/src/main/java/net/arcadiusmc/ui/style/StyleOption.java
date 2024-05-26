package net.arcadiusmc.ui.style;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Color;

public record StyleOption<T>(String name, int id, Class<T> type) {

  public static final Map<String, StyleOption<?>> OPTIONS = new HashMap<>();
  public static final StyleOption[] OPTION_ID_LOOKUP;

  public static final StyleOption<TextColorFunction> COLOR = create("color", TextColorFunction.class);

  public static final StyleOption<Color> BACKGROUND_COLOR = create("background-color", Color.class);
  public static final StyleOption<Color> BORDER_COLOR = create("border-color", Color.class);

  public static final StyleOption<Float> MARGIN_BOTTOM = create("margin-bottom", Float.class);
  public static final StyleOption<Float> MARGIN_TOP = create("margin-top", Float.class);
  public static final StyleOption<Float> MARGIN_LEFT = create("margin-left", Float.class);
  public static final StyleOption<Float> MARGIN_RIGHT = create("margin-right", Float.class);

  public static final StyleOption<Float> BORDER_BOTTOM = create("border-bottom", Float.class);
  public static final StyleOption<Float> BORDER_TOP = create("border-top", Float.class);
  public static final StyleOption<Float> BORDER_LEFT = create("border-left", Float.class);
  public static final StyleOption<Float> BORDER_RIGHT = create("border-right", Float.class);

  public static final StyleOption<Float> PADDING_BOTTOM = create("padding-bottom", Float.class);
  public static final StyleOption<Float> PADDING_TOP = create("padding-top", Float.class);
  public static final StyleOption<Float> PADDING_LEFT = create("padding-left", Float.class);
  public static final StyleOption<Float> PADDING_RIGHT = create("padding-right", Float.class);

  static {
    OPTION_ID_LOOKUP = new StyleOption[OPTIONS.size()];
    for (StyleOption<?> value : OPTIONS.values()) {
      OPTION_ID_LOOKUP[value.id] = value;
    }
  }

  private static <T> StyleOption<T> create(String name, Class<T> type) {
    Preconditions.checkState(!OPTIONS.containsKey(name), "Name already used: '%s'", name);

    StyleOption<T> option = new StyleOption<>(name, OPTIONS.size(), type);
    OPTIONS.put(name, option);

    return option;
  }
}
