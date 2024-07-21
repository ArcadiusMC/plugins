package net.arcadiusmc.ui.style;

import java.util.function.BiConsumer;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.struct.Align;
import net.arcadiusmc.ui.struct.Node;
import net.arcadiusmc.ui.style.Rule.StyleFunction;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;

public final class Rules {
  private Rules() {}

  public static final Registry<Rule<?>> REGISTRY;

  public static final Rule<Color> TEXT_COLOR = Rule.builder(Color.class)
      .defaultValue(Color.BLACK)
      .cascading(true)
      .layoutAffecting(false)
      .contentAffecting(true)
      .build();

  public static final Rule<Color> BACKGROUND_COLOR = Rule.builder(Color.class)
      .defaultValue(Color.WHITE)
      .cascading(true)
      .layoutAffecting(false)
      .function((n, screen, color) -> n.getRenderElement().setBackgroundColor(color))
      .build();

  public static final Rule<Color> BORDER_COLOR = Rule.builder(Color.class)
      .defaultValue(Color.BLACK)
      .cascading(false)
      .layoutAffecting(false)
      .function((n, screen, color) -> n.getRenderElement().setBorderColor(color))
      .build();

  public static final Rule<Color> OUTLINE_COLOR = Rule.builder(Color.class)
      .defaultValue(Color.BLACK)
      .cascading(false)
      .layoutAffecting(false)
      .function((n, screen, color) -> n.getRenderElement().setOutlineColor(color))
      .build();

  public static final Rule<Align> ALIGN_DIRECTION = Rule.builder(Align.class)
      .defaultValue(Align.Y)
      .cascading(true)
      .layoutAffecting(true)
      .function((n, screen, align) -> n.setDirection(align))
      .build();

  public static final Rule<Boolean> TEXT_SHADOW = Rule.builder(Boolean.class)
      .defaultValue(false)
      .cascading(false)
      .layoutAffecting(false)
      .contentAffecting(true)
      .function((n,s,v) -> n.getRenderElement().setTextShadowed(v))
      .build();

  public static final Rule<Boolean> BOLD = Rule.builder(Boolean.class)
      .defaultValue(false)
      .cascading(false)
      .layoutAffecting(true)
      .contentAffecting(true)
      .build();

  public static final Rule<Boolean> ITALIC = Rule.builder(Boolean.class)
      .defaultValue(false)
      .cascading(false)
      .layoutAffecting(false)
      .contentAffecting(true)
      .build();

  public static final Rule<Boolean> UNDERLINED = Rule.builder(Boolean.class)
      .defaultValue(false)
      .cascading(false)
      .layoutAffecting(false)
      .contentAffecting(true)
      .build();

  public static final Rule<Boolean> STRIKETHROUGH = Rule.builder(Boolean.class)
      .defaultValue(false)
      .cascading(false)
      .layoutAffecting(false)
      .contentAffecting(true)
      .build();

  public static final Rule<Boolean> OBFUSCATED = Rule.builder(Boolean.class)
      .defaultValue(false)
      .cascading(false)
      .layoutAffecting(false)
      .contentAffecting(true)
      .build();

  public static final Rule<DisplayType> DISPLAY = Rule.builder(DisplayType.class)
      .defaultValue(DisplayType.INLINE)
      .cascading(true)
      .layoutAffecting(true)
      .build();

  public static final Rule<StyleValue> SCALE = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.constant(1))
      .cascading(true)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getContentScale().set(v)))
      .build();

  public static final Rule<StyleValue> MIN_WIDTH = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getMinSize().x = v))
      .build();

  public static final Rule<StyleValue> MIN_HEIGHT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getMinSize().y = v))
      .build();

  public static final Rule<StyleValue> MAX_WIDTH = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.constant(Float.MAX_VALUE))
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getMaxSize().x = v))
      .build();

  public static final Rule<StyleValue> MAX_HEIGHT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.constant(Float.MAX_VALUE))
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getMaxSize().y = v))
      .build();

  public static final Rule<StyleValue> BORDER_TOP = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getBorderSize().top = v))
      .build();

  public static final Rule<StyleValue> BORDER_BOTTOM = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getBorderSize().bottom = v))
      .build();

  public static final Rule<StyleValue> BORDER_LEFT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getBorderSize().left = v))
      .build();

  public static final Rule<StyleValue> BORDER_RIGHT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getBorderSize().right = v))
      .build();

  public static final Rule<StyleValue> OUTLINE_TOP = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getOutlineSize().top = v))
      .build();

  public static final Rule<StyleValue> OUTLINE_BOTTOM = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getOutlineSize().bottom = v))
      .build();

  public static final Rule<StyleValue> OUTLINE_LEFT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getOutlineSize().left = v))
      .build();

  public static final Rule<StyleValue> OUTLINE_RIGHT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getOutlineSize().right = v))
      .build();

  public static final Rule<StyleValue> PADDING_TOP = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getPaddingSize().top = v))
      .build();

  public static final Rule<StyleValue> PADDING_BOTTOM = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getPaddingSize().bottom = v))
      .build();

  public static final Rule<StyleValue> PADDING_LEFT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getPaddingSize().left = v))
      .build();

  public static final Rule<StyleValue> PADDING_RIGHT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getRenderElement().getPaddingSize().right = v))
      .build();

  public static final Rule<StyleValue> MARGIN_TOP = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getMargin().top = v))
      .build();

  public static final Rule<StyleValue> MARGIN_BOTTOM = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getMargin().bottom = v))
      .build();

  public static final Rule<StyleValue> MARGIN_LEFT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getMargin().left = v))
      .build();

  public static final Rule<StyleValue> MARGIN_RIGHT = Rule.builder(StyleValue.class)
      .defaultValue(StyleValue.ZERO)
      .cascading(false)
      .layoutAffecting(true)
      .function(createFunction((n, v) -> n.getMargin().right = v))
      .build();

  public static final Rule<Integer> Z_INDEX = Rule.builder(Integer.class)
      .defaultValue(0)
      .cascading(true)
      .layoutAffecting(false)
      .function((n, screen, integer) -> {
        float offset = integer * RenderElement.MACRO_LAYER_DEPTH;
        n.getRenderElement().setZOffset(offset);
      })
      .build();

  static {
    REGISTRY = Registries.newRegistry();
    REGISTRY.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<Rule<?>> value) {
        Rule<?> rule = value.getValue();
        rule.id = value.getId();
        rule.key = value.getKey();
      }

      @Override
      public void onUnregister(Holder<Rule<?>> value) {
        Rule<?> rule = value.getValue();
        rule.id = -1;
        rule.key = null;
      }
    });

    registerAll();
  }

  private static void registerAll() {
    REGISTRY.register("color",            TEXT_COLOR);
    REGISTRY.register("background-color", BACKGROUND_COLOR);
    REGISTRY.register("outline-color",    OUTLINE_COLOR);

    REGISTRY.register("border-color",     BORDER_COLOR);
    REGISTRY.register("align",            ALIGN_DIRECTION);
    REGISTRY.register("text-shadow",      TEXT_SHADOW);
    REGISTRY.register("scale",            SCALE);
    REGISTRY.register("z-index",          Z_INDEX);

    REGISTRY.register("bold",             BOLD);
    REGISTRY.register("italic",           ITALIC);
    REGISTRY.register("underlined",       UNDERLINED);
    REGISTRY.register("strikethrough",    STRIKETHROUGH);
    REGISTRY.register("obfuscated",       OBFUSCATED);

    REGISTRY.register("min-width",        MIN_WIDTH);
    REGISTRY.register("min-height",       MIN_HEIGHT);

    REGISTRY.register("max-width",        MAX_WIDTH);
    REGISTRY.register("max-height",       MAX_HEIGHT);

    REGISTRY.register("border-top",       BORDER_TOP);
    REGISTRY.register("border-bottom",    BORDER_BOTTOM);
    REGISTRY.register("border-left",      BORDER_LEFT);
    REGISTRY.register("border-right",     BORDER_RIGHT);

    REGISTRY.register("outline-top",      OUTLINE_TOP);
    REGISTRY.register("outline-bottom",   OUTLINE_BOTTOM);
    REGISTRY.register("outline-left",     OUTLINE_LEFT);
    REGISTRY.register("outline-right",    OUTLINE_RIGHT);

    REGISTRY.register("padding-top",      PADDING_TOP);
    REGISTRY.register("padding-bottom",   PADDING_BOTTOM);
    REGISTRY.register("padding-left",     PADDING_LEFT);
    REGISTRY.register("padding-right",    PADDING_RIGHT);

    REGISTRY.register("margin-top",       MARGIN_TOP);
    REGISTRY.register("margin-bottom",    MARGIN_BOTTOM);
    REGISTRY.register("margin-left",      MARGIN_LEFT);
    REGISTRY.register("margin-right",     MARGIN_RIGHT);
  }

  private static StyleFunction<StyleValue> createFunction(BiConsumer<Node, Float> f) {
    return (n, screen, styleValue) -> f.accept(n, styleValue.resolve(screen));
  }

  public static TextColor toTextColor(Color c) {
    return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
  }
}
