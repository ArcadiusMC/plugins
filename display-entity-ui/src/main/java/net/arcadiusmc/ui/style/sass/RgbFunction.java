package net.arcadiusmc.ui.style.sass;

import net.arcadiusmc.ui.style.sass.ArgsParser.Argument;
import net.arcadiusmc.ui.util.ParserErrors;
import org.bukkit.Color;

public class RgbFunction implements SassFunction {

  static final int MAX_VALUE = 255;

  private final boolean alphaChannelIncluded;

  public RgbFunction(boolean alphaChannelIncluded) {
    this.alphaChannelIncluded = alphaChannelIncluded;
  }

  @Override
  public Object evaluate(String functionName, ArgsParser parser, ParserErrors errors) {
    Argument<Float> redArg = parser.number();
    Argument<Float> greenArg = parser.number();
    Argument<Float> blueArg = parser.number();
    Argument<Float> alphaArg;

    if (alphaChannelIncluded) {
      alphaArg = parser.number();
    } else {
      alphaArg = null;
    }

    parser.end();

    int r = redArg.value().intValue();
    int g = greenArg.value().intValue();
    int b = blueArg.value().intValue();

    int a = alphaArg == null
        ? MAX_VALUE
        : (int) (MAX_VALUE * alphaArg.value());

    return Color.fromARGB(a, r, g, b);
  }
}
