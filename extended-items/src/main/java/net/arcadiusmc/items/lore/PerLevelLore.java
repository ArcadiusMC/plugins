package net.arcadiusmc.items.lore;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.text.TextWriter;

public class PerLevelLore implements LoreElement {

  private final List<LevelLore> lores;
  private final UnaryOperator<TextWriter> writerModifier;

  public PerLevelLore(List<LevelLore> lores, UnaryOperator<TextWriter> writerModifier) {
    Objects.requireNonNull(lores, "Null lores");

    this.lores = lores;
    this.writerModifier = writerModifier;
  }

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    int level = Level.getLevel(item, -1);

    TextWriter modified;

    if (writerModifier == null) {
      modified = writer;
    } else {
      modified = writerModifier.apply(writer);
    }

    for (LevelLore lore : lores) {
      if (lore.level() != level) {
        continue;
      }

      lore.element().writeLore(item, modified);
    }
  }

}
