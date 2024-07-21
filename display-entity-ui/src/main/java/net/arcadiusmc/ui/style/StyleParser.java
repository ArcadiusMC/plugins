package net.arcadiusmc.ui.style;

import static net.arcadiusmc.ui.style.Token.BRACKET_OPEN;
import static net.arcadiusmc.ui.style.Token.COLON;
import static net.arcadiusmc.ui.style.Token.COMMA;
import static net.arcadiusmc.ui.style.Token.DOLLAR_SIGN;
import static net.arcadiusmc.ui.style.Token.EOF;
import static net.arcadiusmc.ui.style.Token.HEX;
import static net.arcadiusmc.ui.style.Token.HEX_ALPHA;
import static net.arcadiusmc.ui.style.Token.HEX_SHORT;
import static net.arcadiusmc.ui.style.Token.ID;
import static net.arcadiusmc.ui.style.Token.NUMBER;
import static net.arcadiusmc.ui.style.Token.SEMICOLON;
import static net.arcadiusmc.ui.style.Token.SQUIG_CLOSE;
import static net.arcadiusmc.ui.style.Token.SQUIG_OPEN;
import static net.arcadiusmc.ui.style.Token.STRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ui.style.StyleValue.Unit;
import net.arcadiusmc.ui.style.StyleValue.UnitValue;
import net.arcadiusmc.ui.style.Stylesheet.Style;
import net.arcadiusmc.ui.style.TokenStream.ParseMode;
import net.arcadiusmc.ui.style.sass.ArgsParser;
import net.arcadiusmc.ui.style.sass.SassFunction;
import net.arcadiusmc.ui.style.sass.SassFunctions;
import net.arcadiusmc.ui.style.selector.Selector;
import net.arcadiusmc.ui.util.Location;
import net.arcadiusmc.ui.util.StringUtil;
import org.bukkit.Color;

public class StyleParser extends Parser {

  private static final Map<String, Color> COLOR_NAMES = createColorMap();

  @Getter @Setter
  private Map<String, Object> variables = new HashMap<>();

  public StyleParser(StringBuffer in) {
    super(in);
  }

  private void expectStatementEnd(int endType) {
    Token peek = peek();

    if (peek.type() == SEMICOLON) {
      next();
      return;
    }

    if (peek.type() == endType) {
      return;
    }

    errors.err(peek.location(),
        "Expected %s or %s, found %s (Property values must end with a semicolon)",
        Token.typeToString(SEMICOLON),
        Token.typeToString(endType),
        peek.info()
    );
  }

  public void inlineRules(StylePropertyMap set) {
    stream.pushMode(ParseMode.VALUES);

    while (stream.hasNext()) {
      rule(set);
      expectStatementEnd(EOF);
    }

    stream.popMode();
  }

  public StylePropertyMap inlineRules() {
    StylePropertyMap set = new StylePropertyMap();
    inlineRules(set);
    return set;
  }

  public Stylesheet stylesheet() {
    Stylesheet stylesheet = new Stylesheet();

    while (hasNext()) {
      Token peek = peek();

      if (peek.type() == DOLLAR_SIGN) {
        variableDefinition();
        continue;
      }

      style(stylesheet);
    }

    return stylesheet;
  }

  void style(Stylesheet out) {
    List<Selector> selectorList = new ArrayList<>();
    selectorList.add(selector());

    while (peek().type() == COMMA) {
      next();
      selectorList.add(selector());
    }

    StylePropertyMap set = ruleset();

    for (Selector selector : selectorList) {
      Style s = new Style(selector, set);
      out.getStyles().add(s);
    }
  }

  StylePropertyMap ruleset() {
    expect(SQUIG_OPEN);

    stream.pushMode(ParseMode.VALUES);
    StylePropertyMap set = new StylePropertyMap();

    while (peek().type() != SQUIG_CLOSE) {
      rule(set);
      expectStatementEnd(EOF);
    }

    expect(SQUIG_CLOSE);
    stream.popMode();

    return set;
  }

  void variableDefinition() {
    Token prefix = expect(DOLLAR_SIGN);
    Location l = prefix.location();

    Token name = expect(ID);
    expect(COLON);

    Object value = parseValue(String.class);

    if (peek().type() == SEMICOLON) {
      next();
    }

    if (value == null) {
      return;
    }

    if (variables.containsKey(name.value())) {
      errors.warn(l, "Variable named '%s' already defined", name.value());
    }

    variables.put(name.value(), value);
  }

  @SuppressWarnings("unchecked")
  private void rule(StylePropertyMap out) {
    Token id = expect(ID);
    expect(COLON);

    String ruleKey = id.value();
    Objects.requireNonNull(ruleKey);

    switch (ruleKey) {
      case "padding":
      case "margin":
      case "outline":
      case "border":
        rectangleShorthand(ruleKey, out);
        break;

      default:
        Rule<Object> rule = (Rule<Object>) Rules.REGISTRY.orNull(ruleKey);

        Object value = parseValue(rule == null ? Object.class : rule.getType());

        if (rule == null) {
          errors.err(id.location(), "Unknown/unsupported rule '%s'", ruleKey);
          return;
        }

        if (value == null) {
          return;
        }

        if (!rule.getType().isInstance(value)) {
          errors.err(id.location(), "Invalid value for rule '%s': %s", ruleKey, value);
          return;
        }

        out.set(rule, value);
        break;
    }
  }

  private void rectangleShorthand(String key, StylePropertyMap out) {
    Rule<StyleValue> top;
    Rule<StyleValue> right;
    Rule<StyleValue> bottom;
    Rule<StyleValue> left;

    switch (key) {
      case "margin" -> {
        top = Rules.MARGIN_TOP;
        left = Rules.MARGIN_LEFT;
        bottom = Rules.MARGIN_BOTTOM;
        right = Rules.MARGIN_RIGHT;
      }

      case "padding" -> {
        top = Rules.PADDING_TOP;
        left = Rules.PADDING_LEFT;
        bottom = Rules.PADDING_BOTTOM;
        right = Rules.PADDING_RIGHT;
      }

      case "border" -> {
        top = Rules.BORDER_TOP;
        left = Rules.BORDER_LEFT;
        bottom = Rules.BORDER_BOTTOM;
        right = Rules.BORDER_RIGHT;
      }

      case "outline" -> {
        top = Rules.OUTLINE_TOP;
        left = Rules.OUTLINE_LEFT;
        bottom = Rules.OUTLINE_BOTTOM;
        right = Rules.OUTLINE_RIGHT;
      }

      default -> {
        return;
      }
    }

    StyleValue[] values = new StyleValue[4];
    int count = 0;
    boolean err = false;

    for (int i = 0; i < values.length; i++) {
      Token peek = peek();

      if (peek.type() != NUMBER && peek.type() != DOLLAR_SIGN) {
        break;
      }

      StyleValue value = parseAs(StyleValue.class);

      if (value == null) {
        errors.err(peek.location(), "Expected number value/variable");
        err = true;
        continue;
      }

      values[count++] = value;
    }

    if (err) {
      return;
    }

    switch (count) {
      case 1:
        out.set(top, values[0]);
        out.set(right, values[0]);
        out.set(bottom, values[0]);
        out.set(left, values[0]);
        break;
      case 2:
        out.set(top, values[0]);
        out.set(right, values[1]);
        out.set(bottom, values[0]);
        out.set(left, values[1]);
        break;
      case 3:
        out.set(top, values[0]);
        out.set(right, values[1]);
        out.set(bottom, values[2]);
        out.set(left, values[1]);
        break;
      case 4:
        out.set(top, values[0]);
        out.set(right, values[1]);
        out.set(bottom, values[2]);
        out.set(left, values[3]);
        break;

      default:
        return;
    }
  }

  private Object attemptCoercion(Location l, String val, Class<?> hint) {
    if (String.class.isAssignableFrom(hint)) {
      return val;
    }

    Boolean b = StringUtil.parseBoolean(val);

    if (b != null) {
      return b;
    }

    if (hint == Boolean.class || hint == Boolean.TYPE) {
      errors.err(l, "Invalid boolean value '%s'", val);
      return null;
    }

    if (Enum.class.isAssignableFrom(hint)) {
      Enum[] constants = (Enum[]) hint.getEnumConstants();
      String underscoredVal = val.replace("-", "_");

      for (Enum constant : constants) {
        String name = constant.name();

        if (name.equalsIgnoreCase(val) || name.equalsIgnoreCase(underscoredVal)) {
          return constant;
        }
      }

      errors.err(l, "Invalid %s value '%s'", hint.getSimpleName(), val);
      return null;
    }

    Color c = COLOR_NAMES.get(val.toLowerCase());
    if (c == null) {
      errors.err(l, "Unknown color '%s'", val);
      return Color.BLACK;
    }

    return c;
  }

  private Object evaluateFunction(String functionName) {
    Token start = expect(BRACKET_OPEN);
    Location l = start.location();

    SassFunction function;

    switch (functionName.toLowerCase()) {
      case "lighten":
        function = SassFunctions.LIGHTEN;
        break;
      case "darken":
        function = SassFunctions.DARKEN;
        break;
      case "rgb":
        function = SassFunctions.RGB;
        break;
      case "rgba":
        function = SassFunctions.RGBA;
        break;

      default:
        errors.err(l, "Unknown/unsupported function '%s'", functionName);
        return null;
    }

    ArgsParser parser = new ArgsParser(this);
    return function.evaluate(functionName, parser, errors);
  }

  public  <T> T parseAs(Class<T> type) {
    Object o = parseValue(type);

    if (!type.isInstance(o)) {
      return null;
    }

    return type.cast(o) ;
  }

  @SuppressWarnings("null")
  private Object parseValue(Class<?> hint) {
    Token next = next();
    String val = next.value();

    return switch (next.type()) {
      case STRING -> attemptCoercion(next.location(), val, hint);

      case ID -> {
        if (peek().type() == BRACKET_OPEN) {
          yield evaluateFunction(val);
        }

        yield attemptCoercion(next.location(), val, hint);
      }

      case DOLLAR_SIGN -> {
        Token var = expect(ID);
        Object varValue = variables.get(var.value());

        if (varValue == null) {
          errors.err(var.location(), "Unknown variable '%s'", var.value());
        } else if (varValue instanceof String str) {
          yield attemptCoercion(next.location(), str, hint);
        }

        yield varValue;
      }

      case HEX -> {
        int rgb = Integer.parseUnsignedInt(val, 16);
        yield Color.fromRGB(rgb);
      }

      case HEX_SHORT -> {
        StringBuilder buffer = new StringBuilder();

        for (char ch: val.toCharArray()) {
          buffer.append(ch);
          buffer.append(ch);
        }

        int rgb = Integer.parseUnsignedInt(buffer, 0, buffer.length(), 16);
        yield Color.fromRGB(rgb);
      }

      case HEX_ALPHA -> {
        int argb = Integer.parseUnsignedInt(val, 16);
        yield Color.fromARGB(argb);
      }

      case NUMBER -> {
        float f = Float.parseFloat(val);

        Token peeked = peek();
        int ptype = peeked.type();

        Unit unit;

        if (hint == Integer.class) {
          yield (int) f;
        }
        if (hint == Long.class) {
          yield (long) f;
        }
        if (hint == Float.class) {
          yield f;
        }
        if (hint == Double.class) {
          yield (double) f;
        }

        if (ptype == ID) {
          String pvalue = peeked.value();
          assert pvalue != null;

          unit = switch (pvalue) {
            case "px" -> Unit.PX;
            case "vw" -> Unit.SCREEN_WIDTH;
            case "vh" -> Unit.SCREEN_HEIGHT;
            case "ch" -> Unit.CHARACTER;
            default -> Unit.NONE;
          };
        } else {
          unit = Unit.NONE;
        }

        if (unit != Unit.NONE) {
          next();
        }

        yield new UnitValue(f, unit);
      }

      default -> {
        errors.err(next.location(), "Invalid value type");
        yield null;
      }
    };
  }

  private static Map<String, Color> createColorMap() {
    Map<String, Color> map = new HashMap<>();

    map.put("aliceblue",             Color.fromRGB(0xf0f8ff));
    map.put("antiquewhite",          Color.fromRGB(0xfaebd7));
    map.put("aqua",                  Color.fromRGB(0x00ffff));
    map.put("aquamarine",            Color.fromRGB(0x7fffd4));
    map.put("azure",                 Color.fromRGB(0xf0ffff));
    map.put("beige",                 Color.fromRGB(0xf5f5dc));
    map.put("bisque",                Color.fromRGB(0xffe4c4));
    map.put("black",                 Color.fromRGB(0x000000));
    map.put("blanchedalmond",        Color.fromRGB(0xffebcd));
    map.put("blue",                  Color.fromRGB(0x0000ff));
    map.put("blueviolet",            Color.fromRGB(0x8a2be2));
    map.put("brown",                 Color.fromRGB(0xa52a2a));
    map.put("burlywood",             Color.fromRGB(0xdeb887));
    map.put("cadetblue",             Color.fromRGB(0x5f9ea0));
    map.put("chartreuse",            Color.fromRGB(0x7fff00));
    map.put("chocolate",             Color.fromRGB(0xd2691e));
    map.put("coral",                 Color.fromRGB(0xff7f50));
    map.put("cornflowerblue",        Color.fromRGB(0x6495ed));
    map.put("cornsilk",              Color.fromRGB(0xfff8dc));
    map.put("crimson",               Color.fromRGB(0xdc143c));
    map.put("cyan",                  Color.fromRGB(0x00ffff));
    map.put("darkblue",              Color.fromRGB(0x00008b));
    map.put("darkcyan",              Color.fromRGB(0x008b8b));
    map.put("darkgoldenrod",         Color.fromRGB(0xb8860b));
    map.put("darkgray",              Color.fromRGB(0xa9a9a9));
    map.put("darkgrey",              Color.fromRGB(0xa9a9a9));
    map.put("darkgreen",             Color.fromRGB(0x006400));
    map.put("darkkhaki",             Color.fromRGB(0xbdb76b));
    map.put("darkmagenta",           Color.fromRGB(0x8b008b));
    map.put("darkolivegreen",        Color.fromRGB(0x556b2f));
    map.put("darkorange",            Color.fromRGB(0xff8c00));
    map.put("darkorchid",            Color.fromRGB(0x9932cc));
    map.put("darkred",               Color.fromRGB(0x8b0000));
    map.put("darksalmon",            Color.fromRGB(0xe9967a));
    map.put("darkseagreen",          Color.fromRGB(0x8fbc8f));
    map.put("darkslateblue",         Color.fromRGB(0x483d8b));
    map.put("darkslategray",         Color.fromRGB(0x2f4f4f));
    map.put("darkslategrey",         Color.fromRGB(0x2f4f4f));
    map.put("darkturquoise",         Color.fromRGB(0x00ced1));
    map.put("darkviolet",            Color.fromRGB(0x9400d3));
    map.put("deeppink",              Color.fromRGB(0xff1493));
    map.put("deepskyblue",           Color.fromRGB(0x00bfff));
    map.put("dimgray",               Color.fromRGB(0x696969));
    map.put("dimgrey",               Color.fromRGB(0x696969));
    map.put("dodgerblue",            Color.fromRGB(0x1e90ff));
    map.put("firebrick",             Color.fromRGB(0xb22222));
    map.put("floralwhite",           Color.fromRGB(0xfffaf0));
    map.put("forestgreen",           Color.fromRGB(0x228b22));
    map.put("fuchsia",               Color.fromRGB(0xff00ff));
    map.put("gainsboro",             Color.fromRGB(0xdcdcdc));
    map.put("ghostwhite",            Color.fromRGB(0xf8f8ff));
    map.put("gold",                  Color.fromRGB(0xffd700));
    map.put("goldenrod",             Color.fromRGB(0xdaa520));
    map.put("gray",                  Color.fromRGB(0x808080));
    map.put("grey",                  Color.fromRGB(0x808080));
    map.put("green",                 Color.fromRGB(0x008000));
    map.put("greenyellow",           Color.fromRGB(0xadff2f));
    map.put("honeydew",              Color.fromRGB(0xf0fff0));
    map.put("hotpink",               Color.fromRGB(0xff69b4));
    map.put("indianred",             Color.fromRGB(0xcd5c5c));
    map.put("indigo",                Color.fromRGB(0x4b0082));
    map.put("ivory",                 Color.fromRGB(0xfffff0));
    map.put("khaki",                 Color.fromRGB(0xf0e68c));
    map.put("lavender",              Color.fromRGB(0xe6e6fa));
    map.put("lavenderblush",         Color.fromRGB(0xfff0f5));
    map.put("lawngreen",             Color.fromRGB(0x7cfc00));
    map.put("lemonchiffon",          Color.fromRGB(0xfffacd));
    map.put("lightblue",             Color.fromRGB(0xadd8e6));
    map.put("lightcoral",            Color.fromRGB(0xf08080));
    map.put("lightcyan",             Color.fromRGB(0xe0ffff));
    map.put("lightgoldenrodyellow",  Color.fromRGB(0xfafad2));
    map.put("lightgray",             Color.fromRGB(0xd3d3d3));
    map.put("lightgrey",             Color.fromRGB(0xd3d3d3));
    map.put("lightgreen",            Color.fromRGB(0x90ee90));
    map.put("lightpink",             Color.fromRGB(0xffb6c1));
    map.put("lightsalmon",           Color.fromRGB(0xffa07a));
    map.put("lightseagreen",         Color.fromRGB(0x20b2aa));
    map.put("lightskyblue",          Color.fromRGB(0x87cefa));
    map.put("lightslategray",        Color.fromRGB(0x778899));
    map.put("lightslategrey",        Color.fromRGB(0x778899));
    map.put("lightsteelblue",        Color.fromRGB(0xb0c4de));
    map.put("lightyellow",           Color.fromRGB(0xffffe0));
    map.put("lime",                  Color.fromRGB(0x00ff00));
    map.put("limegreen",             Color.fromRGB(0x32cd32));
    map.put("linen",                 Color.fromRGB(0xfaf0e6));
    map.put("magenta",               Color.fromRGB(0xff00ff));
    map.put("maroon",                Color.fromRGB(0x800000));
    map.put("mediumaquamarine",      Color.fromRGB(0x66cdaa));
    map.put("mediumblue",            Color.fromRGB(0x0000cd));
    map.put("mediumorchid",          Color.fromRGB(0xba55d3));
    map.put("mediumpurple",          Color.fromRGB(0x9370db));
    map.put("mediumseagreen",        Color.fromRGB(0x3cb371));
    map.put("mediumslateblue",       Color.fromRGB(0x7b68ee));
    map.put("mediumspringgreen",     Color.fromRGB(0x00fa9a));
    map.put("mediumturquoise",       Color.fromRGB(0x48d1cc));
    map.put("mediumvioletred",       Color.fromRGB(0xc71585));
    map.put("midnightblue",          Color.fromRGB(0x191970));
    map.put("mintcream",             Color.fromRGB(0xf5fffa));
    map.put("mistyrose",             Color.fromRGB(0xffe4e1));
    map.put("moccasin",              Color.fromRGB(0xffe4b5));
    map.put("navajowhite",           Color.fromRGB(0xffdead));
    map.put("navy",                  Color.fromRGB(0x000080));
    map.put("oldlace",               Color.fromRGB(0xfdf5e6));
    map.put("olive",                 Color.fromRGB(0x808000));
    map.put("olivedrab",             Color.fromRGB(0x6b8e23));
    map.put("orange",                Color.fromRGB(0xffa500));
    map.put("orangered",             Color.fromRGB(0xff4500));
    map.put("orchid",                Color.fromRGB(0xda70d6));
    map.put("palegoldenrod",         Color.fromRGB(0xeee8aa));
    map.put("palegreen",             Color.fromRGB(0x98fb98));
    map.put("paleturquoise",         Color.fromRGB(0xafeeee));
    map.put("palevioletred",         Color.fromRGB(0xdb7093));
    map.put("papayawhip",            Color.fromRGB(0xffefd5));
    map.put("peachpuff",             Color.fromRGB(0xffdab9));
    map.put("peru",                  Color.fromRGB(0xcd853f));
    map.put("pink",                  Color.fromRGB(0xffc0cb));
    map.put("plum",                  Color.fromRGB(0xdda0dd));
    map.put("powderblue",            Color.fromRGB(0xb0e0e6));
    map.put("purple",                Color.fromRGB(0x800080));
    map.put("rebeccapurple",         Color.fromRGB(0x663399));
    map.put("red",                   Color.fromRGB(0xff0000));
    map.put("rosybrown",             Color.fromRGB(0xbc8f8f));
    map.put("royalblue",             Color.fromRGB(0x4169e1));
    map.put("saddlebrown",           Color.fromRGB(0x8b4513));
    map.put("salmon",                Color.fromRGB(0xfa8072));
    map.put("sandybrown",            Color.fromRGB(0xf4a460));
    map.put("seagreen",              Color.fromRGB(0x2e8b57));
    map.put("seashell",              Color.fromRGB(0xfff5ee));
    map.put("sienna",                Color.fromRGB(0xa0522d));
    map.put("silver",                Color.fromRGB(0xc0c0c0));
    map.put("skyblue",               Color.fromRGB(0x87ceeb));
    map.put("slateblue",             Color.fromRGB(0x6a5acd));
    map.put("slategray",             Color.fromRGB(0x708090));
    map.put("slategrey",             Color.fromRGB(0x708090));
    map.put("snow",                  Color.fromRGB(0xfffafa));
    map.put("springgreen",           Color.fromRGB(0x00ff7f));
    map.put("steelblue",             Color.fromRGB(0x4682b4));
    map.put("tan",                   Color.fromRGB(0xd2b48c));
    map.put("teal",                  Color.fromRGB(0x008080));
    map.put("thistle",               Color.fromRGB(0xd8bfd8));
    map.put("tomato",                Color.fromRGB(0xff6347));
    map.put("turquoise",             Color.fromRGB(0x40e0d0));
    map.put("violet",                Color.fromRGB(0xee82ee));
    map.put("wheat",                 Color.fromRGB(0xf5deb3));
    map.put("white",                 Color.fromRGB(0xffffff));
    map.put("whitesmoke",            Color.fromRGB(0xf5f5f5));
    map.put("yellow",                Color.fromRGB(0xffff00));
    map.put("yellowgreen",           Color.fromRGB(0x9acd32));

    return Collections.unmodifiableMap(map);
  }
}
