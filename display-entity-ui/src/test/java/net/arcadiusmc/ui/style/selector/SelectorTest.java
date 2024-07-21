package net.arcadiusmc.ui.style.selector;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import net.arcadiusmc.ui.style.Parser;
import net.arcadiusmc.ui.util.ParserErrors;
import net.arcadiusmc.ui.util.ParserErrors.Error;
import net.arcadiusmc.ui.util.ParserException;
import org.junit.jupiter.api.Test;

class SelectorTest {

  static final String[] SELECTORS = {
      ".class-name",
      ".class-name .other-class",
      ".class-name .other-class[attr=\"value\"]",
      ".class-name .other-class[attr~=\"value\"]",
      ".class-name .other-class[attr*=\"value\"]",
      ".class-name .other-class[attr^=\"value\"]",
      ".class-name .other-class[attr|=\"value\"]",
      ".class-name .other-class[attr|=\"value\"][second=\"other-value\"]",
      ".class-name .other-class[attr|=\"value\"][second=\"other-value\"]:hover",
      ".class-name .other-class[attr|=\"value\"][second=\"other-value\"]:click",
      ".class-name:hover .other-class[attr|=\"value\"][second=\"other-value\"]:active",
      "div",
      "div div[id=\"facts\"]",
      "div #idddd",
      "#id-element",
      "*"
  };

  @Test
  void testSpecificity() {
    Selector[] selectors = new Selector[SELECTORS.length];
    for (int i = 0; i < SELECTORS.length; i++) {
      selectors[i] = parse(SELECTORS[i]);
    }

    Arrays.sort(selectors);

    System.out.println("ordered list: ");
    for (Selector selector : selectors) {
      System.out.println(selector);
    }
  }

  @Test
  void testCanParse() {
    for (String selector : SELECTORS) {
      tryParse(selector);
    }
  }

  Selector parse(String in) {
    StringBuffer buffer = new StringBuffer(in);
    Parser parser = new Parser(buffer);
    ParserErrors errors = parser.getErrors();

    Selector selector;

    try {
      selector = parser.selector();
    } catch (ParserException exc) {
      selector = null;
      // Ignored
    }

    if (errors.isErrorPresent()) {
      StringBuilder builder = new StringBuilder();

      for (Error error : errors.getErrors()) {
        builder
            .append('\n')
            .append("[").append(error.level()).append("] ")
            .append(error.message());
      }

      System.err.println(builder);
      fail("Parser failure");
    }

    return selector;
  }

  void tryParse(String in) {
    StringBuffer buffer = new StringBuffer(in);
    Parser parser = new Parser(buffer);
    ParserErrors errors = parser.getErrors();

    Selector selector;

    try {
      selector = parser.selector();
    } catch (ParserException exc) {
      selector = null;
      // Ignored
    }

    if (errors.isErrorPresent()) {
      StringBuilder builder = new StringBuilder();

      for (Error error : errors.getErrors()) {
        builder
            .append('\n')
            .append("[").append(error.level()).append("] ")
            .append(error.message());
      }

      System.err.println(builder);
      fail("Parser failure");
    }

    System.out.println(selector);
  }
}