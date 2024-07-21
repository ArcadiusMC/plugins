package net.arcadiusmc.ui.style;

import static net.arcadiusmc.ui.style.Token.COLON;
import static net.arcadiusmc.ui.style.Token.DOT;
import static net.arcadiusmc.ui.style.Token.EQUALS;
import static net.arcadiusmc.ui.style.Token.HASHTAG;
import static net.arcadiusmc.ui.style.Token.ID;
import static net.arcadiusmc.ui.style.Token.SQUARE_CLOSE;
import static net.arcadiusmc.ui.style.Token.SQUARE_OPEN;
import static net.arcadiusmc.ui.style.Token.SQUIGLY;
import static net.arcadiusmc.ui.style.Token.STAR;
import static net.arcadiusmc.ui.style.Token.STRING;
import static net.arcadiusmc.ui.style.Token.UP_ARROW;
import static net.arcadiusmc.ui.style.Token.DOLLAR_SIGN;
import static net.arcadiusmc.ui.style.Token.WALL;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.arcadiusmc.ui.style.TokenStream.ParseMode;
import net.arcadiusmc.ui.style.selector.AttributedNode;
import net.arcadiusmc.ui.style.selector.AttributedNode.AttributeTest;
import net.arcadiusmc.ui.style.selector.AttributedNode.Operation;
import net.arcadiusmc.ui.style.selector.ClassNode;
import net.arcadiusmc.ui.style.selector.IdNode;
import net.arcadiusmc.ui.style.selector.PseudoClassNode;
import net.arcadiusmc.ui.style.selector.PseudoClassNode.PseudoClass;
import net.arcadiusmc.ui.style.selector.Selector;
import net.arcadiusmc.ui.style.selector.SelectorNode;
import net.arcadiusmc.ui.style.selector.TagNameNode;
import net.arcadiusmc.ui.util.ParserErrors;

public class Parser {

  @Getter
  protected final ParserErrors errors;
  protected final TokenStream stream;

  public Parser(StringBuffer in) {
    this.errors = new ParserErrors(in);
    this.stream = new TokenStream(in, errors);
  }

  public boolean hasNext() {
    return stream.hasNext();
  }

  public Token next() {
    return stream.next();
  }

  public Token peek() {
    return stream.peek();
  }

  public Token expect(int tokenType) {
    return stream.expect(tokenType);
  }

  public Selector selector() {
    stream.pushMode(ParseMode.TOKENS);

    List<SelectorNode> nodes = new ArrayList<>();

    while (true) {
      SelectorNode node;
      Token p = peek();

      if (p.type() == STAR) {
        next();
        node = SelectorNode.MATCH_ALL;
      } else if (p.type() == DOT) {
        next();
        node = new ClassNode(stream.expect(ID).value());
      } else if (p.type() == HASHTAG) {
        next();
        node = new IdNode(stream.expect(ID).value());
      } else if (p.type() == ID) {
        next();
        node = new TagNameNode(p.value());
      } else {
        break;
      }

      p = peek();

      if (p.type() == SQUARE_OPEN) {
        node = attributed(node);
      }

      if (p.type() == COLON) {
        node = pseudoClass(node);
      }

      nodes.add(node);
    }

    stream.popMode();

    SelectorNode[] nodeArr = nodes.toArray(SelectorNode[]::new);
    return new Selector(nodeArr);
  }

  private PseudoClassNode pseudoClass(SelectorNode base) {
    expect(COLON);
    Token id = expect(ID);

    PseudoClass pseudoClass = switch (id.value()) {
      case "hover" -> PseudoClass.HOVER;
      case "click", "active" -> PseudoClass.ACTIVE;
      case "enabled" -> PseudoClass.ENABLED;
      case "disabled" -> PseudoClass.DISABLED;

      default -> {
        errors.fatal(id.location(), "Invalid/unsupported pseudo class :%s", id.value());
        yield null;
      }
    };

    return new PseudoClassNode(base, pseudoClass);
  }

  private AttributedNode attributed(SelectorNode base) {
    expect(SQUARE_OPEN);

    List<AttributeTest> tests = new ArrayList<>();

    while (true) {
      Token attrNameT = expect(ID);
      String attrValue;

      Token peek = peek();
      int ptype = peek.type();

      Operation op = switch (ptype) {
        case EQUALS -> Operation.EQUALS;
        case SQUIGLY -> Operation.CONTAINS_WORD;
        case WALL -> Operation.DASH_PREFIXED;
        case UP_ARROW -> Operation.STARTS_WITH;
        case DOLLAR_SIGN -> Operation.ENDS_WITH;
        case STAR -> Operation.CONTAINS_SUBSTRING;
        default -> Operation.HAS;
      };

      boolean expectValue;

      switch (op) {
        case CONTAINS_WORD:
        case DASH_PREFIXED:
        case STARTS_WITH:
        case ENDS_WITH:
        case CONTAINS_SUBSTRING:
          next();
          expect(EQUALS);
          expectValue = true;
          break;

        case EQUALS:
          next();
          expectValue = true;
          break;

        default:
          expectValue = false;
          break;
      }

      if (expectValue) {
        Token valT = expect(STRING);
        attrValue = valT.value();
      } else {
        attrValue = null;
      }

      expect(SQUARE_CLOSE);

      AttributeTest test = new AttributeTest(attrNameT.value(), op, attrValue);
      tests.add(test);

      if (peek().type() != SQUARE_OPEN) {
        break;
      } else {
        next();
      }
    }

    return new AttributedNode(base, tests);
  }
}
