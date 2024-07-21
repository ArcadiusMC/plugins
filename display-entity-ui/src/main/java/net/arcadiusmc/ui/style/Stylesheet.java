package net.arcadiusmc.ui.style;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.ToString;
import net.arcadiusmc.ui.style.selector.Selector;
import org.jetbrains.annotations.NotNull;

@Getter
@ToString
public class Stylesheet {

  private final List<Style> styles = new ArrayList<>();

  public record Style(Selector selector, StylePropertyMap rules) implements Comparable<Style> {

    @Override
    public int compareTo(@NotNull Stylesheet.Style o) {
      return selector.compareTo(o.selector);
    }
  }
}
