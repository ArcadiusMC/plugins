package net.arcadiusmc.ui.style;

import static net.arcadiusmc.ui.render.RenderElement.CHAR_PX_SIZE;

import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.ui.math.Screen;

public interface StyleValue {

  StyleValue ZERO = screen -> 0.0f;

  static StyleValue constant(float value) {
    return value == 0.0f ? ZERO : new Basic(value);
  }

  float resolve(Screen screen);

  enum Unit {
    NONE,
    SCREEN_WIDTH,
    SCREEN_HEIGHT,
    CHARACTER,
    PX,
    ;
  }

  record UnitValue(float base, Unit unit) implements StyleValue {

    static final float LEN0 = TextInfo.getCharPxWidth('0');
    static final float LEN0_PX = LEN0 * CHAR_PX_SIZE;

    @Override
    public float resolve(Screen screen) {
      return switch (unit) {
        case NONE -> base;
        case PX -> base * CHAR_PX_SIZE;
        case CHARACTER -> base * LEN0_PX;
        case SCREEN_WIDTH -> (base / 100.0f) * screen.getWidth();
        case SCREEN_HEIGHT -> (base / 100.0f) * screen.getHeight();
      };
    }

    @Override
    public String toString() {
      String unitString = switch (unit) {
        case PX -> "px";
        case SCREEN_HEIGHT -> "vh";
        case SCREEN_WIDTH -> "vw";
        case CHARACTER -> "ch";
        default -> "";
      };

      return getClass().getSimpleName() + "(" + base + unitString + ")";
    }
  }

  record Basic(float v) implements StyleValue {

    @Override
    public float resolve(Screen screen) {
      return v;
    }
  }
}
