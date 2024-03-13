package net.arcadiusmc.items;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;

@Getter @Setter
public class Level extends ItemComponent implements LoreElement {

  public static final int STARTING_LEVEL = 1;
  static final String LEVEL_TAG = "level";

  private final int max;
  private int level;

  public Level(int max) {
    this.max = max;
    this.level = STARTING_LEVEL;
  }

  public static boolean levelMatches(ExtendedItem item, int atLeast) {
    return getLevel(item) >= atLeast;
  }

  public static int getLevel(ExtendedItem item) {
    return getLevel(item, -1);
  }

  public static int getLevel(ExtendedItem item, int orElse) {
    return item.getComponent(Level.class).map(Level::getLevel).orElse(orElse);
  }

  @Override
  public void save(CompoundTag tag) {
    tag.putInt(LEVEL_TAG, level);
  }

  @Override
  public void load(CompoundTag tag) {
    level = tag.getInt(LEVEL_TAG);
  }

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    String key = "itemsPlugin.level." + (max == -1 ? "uncapped" : "capped");

    Component message = Messages.render(key)
        .addValue("level", level)
        .addValue("max", max)
        .asComponent();

    writer.line(message);
  }
}
