package net.arcadiusmc.items.lore;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.text.TextWriter;
import org.jetbrains.annotations.NotNull;

public record LevelLore(int level, LoreElement element)
    implements Comparable<LevelLore>, LoreElement
{

  @Override
  public int compareTo(@NotNull LevelLore o) {
    return Integer.compare(level, o.level);
  }

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    if (!Level.levelMatches(item, level)) {
      return;
    }

    element.writeLore(item, writer);
  }
}
