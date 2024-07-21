package net.arcadiusmc.ui.style.sass;

import net.arcadiusmc.ui.style.Token;
import net.arcadiusmc.ui.style.sass.ArgsParser.Argument;
import net.arcadiusmc.ui.util.ParserErrors;
import org.bukkit.Color;
import org.joml.Math;

public class BrightnessFunction implements SassFunction {

  private static final float DEFAULT_PROG = 0.50f;

  private final boolean darken;

  public BrightnessFunction(boolean darken) {
    this.darken = darken;
  }

  @Override
  public Object evaluate(String functionName, ArgsParser parser, ParserErrors errors) {
    Argument<Color> arg = parser.value(Color.class);
    float prog;

    if (parser.hasMoreArguments()) {
      Argument<Float> floatArg = parser.suffixedNumber(Token.PERCENT);
      prog = floatArg.value() / 100.0f;

      if (prog < 0.0f) {
        prog = 0.0f;
      } else if (prog > 1.0f) {
        prog = 1.0f;
      }
    } else {
      prog = DEFAULT_PROG;
    }

    parser.end();

    if (arg.value() == null) {
      errors.err(arg.location(), "Function '%s' requires 1 color argument", functionName);
      return null;
    }

    Color c = arg.value();
    int min = darken ? -1 : 1;

    int r = applyChannel(prog, min, c.getRed());
    int g = applyChannel(prog, min, c.getGreen());
    int b = applyChannel(prog, min, c.getBlue());

    return Color.fromARGB(c.getAlpha(), r, g, b);
  }

  private static int applyChannel(float prog, int mul, int color) {
    int channel = (int) (color + ((prog * color) * mul));
    return Math.clamp(0, 255, channel);
  }
}
