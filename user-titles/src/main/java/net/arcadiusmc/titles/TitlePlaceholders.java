package net.arcadiusmc.titles;

import java.util.Optional;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.text.placeholder.TextPlaceholder;

public class TitlePlaceholders {

  static final String TITLE_PLACEHOLDER = "rank";
  static final TextPlaceholder RANK = (match, render) -> {
    if (match.isEmpty()) {
      return null;
    }

    Optional<Title> rankOpt = Titles.REGISTRY.get(match);
    return rankOpt.map(Title::getTruncatedPrefix).orElse(null);
  };

  static void registerAll() {
    Placeholders.addDefault(TITLE_PLACEHOLDER, RANK);
  }

  static void unregister() {
    Placeholders.removeDefault(TITLE_PLACEHOLDER);
  }
}
