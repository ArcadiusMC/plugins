package net.arcadiusmc.ui.style.sass;

import net.arcadiusmc.ui.style.StyleParser;
import net.arcadiusmc.ui.style.Token;
import net.arcadiusmc.ui.util.Location;

public class ArgsParser {

  private final StyleParser parser;
  private int argCount = 0;

  public ArgsParser(StyleParser parser) {
    this.parser = parser;
  }

  public void end() {
    parser.expect(Token.BRACKET_CLOSE);
  }

  public boolean hasMoreArguments() {
    return parser.peek().type() != Token.BRACKET_CLOSE;
  }

  private void preParse() {
    if (argCount >= 1 && parser.peek().type() == Token.COMMA) {
      parser.next();
    }

    argCount++;
  }

  public Argument<String> identifier() {
    preParse();

    Token id = parser.expect(Token.ID);
    return new Argument<>(id.value(), id.location());
  }

  public Argument<Float> number() {
    preParse();

    Token tok = parser.expect(Token.NUMBER);
    return new Argument<>(Float.parseFloat(tok.value()), tok.location());
  }

  public Argument<Float> suffixedNumber(int tokenType) {
    Argument<Float> arg = number();
    parser.expect(tokenType);
    return arg;
  }

  public Argument<Float> suffixedNumber(String unit) {
    Argument<Float> arg = number();
    Token suffix = parser.expect(Token.ID);

    if (suffix.value().equals(unit)) {
      return arg;
    }

    parser.getErrors().err(suffix.location(), "Expected unit '%s'", unit);
    return arg;
  }

  public <T> Argument<T> value(Class<T> type) {
    preParse();

    Location l = parser.peek().location();
    T value = parser.parseAs(type);

    return new Argument<>(value, l);
  }


  record Argument<T> (T value, Location location) {

  }
}
