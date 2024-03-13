package net.arcadiusmc.items.lore;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.text.TextWriter;

public class FlavourTextElement implements LoreElement {

  final LevelLore[] texts;
  final LoreElement borders;

  public FlavourTextElement(LevelLore[] texts, LoreElement borders) {
    Objects.requireNonNull(texts, "Null texts");

    this.texts = texts;
    this.borders = borders;

    Arrays.sort(texts, Comparator.reverseOrder());
  }

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    int level = Level.getLevel(item, Level.STARTING_LEVEL);
    LevelLore flavourText = getText(level);

    // By design, other components might visually be
    // expecting that there's a border below or above them,
    // if there's no flavour text, a border must at least
    // still be drawn
    if (borders != null) {
      borders.writeLore(item, writer);
    }

    if (flavourText == null) {
      return;
    }

    flavourText.element().writeLore(item, writer);

    if (borders != null) {
      borders.writeLore(item, writer);
    }
  }

  LevelLore getText(int level) {
    if (texts.length < 1) {
      return null;
    }

    if (texts.length == 1) {
      return texts[0];
    }

    for (LevelLore text : texts) {
      if (level >= text.level()) {
        return text;
      }
    }

    return null;
  }
}
