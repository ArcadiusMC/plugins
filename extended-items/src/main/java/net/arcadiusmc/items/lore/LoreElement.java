package net.arcadiusmc.items.lore;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;

public interface LoreElement {

  LoreElement DOUBLE_EMPTY_LINE = (item, writer) -> {
    writer.newLine();
    writer.newLine();
  };

  LoreElement SINGLE_EMPTY_LINE = (item, writer) -> {
    writer.newLine();
  };

  LoreElement BORDER = (item, writer) -> {
    writer.line(Messages.renderText("itemsPlugin.border"));
  };

  static LoreElement ifNotMaxLevel(LoreElement element) {
    return (item, writer) -> {
      item.getComponent(Level.class).ifPresent(level -> {
        int max = level.getMax();

        if (max != -1 && level.getLevel() >= max) {
          return;
        }

        element.writeLore(item, writer);
      });
    };
  }

  void writeLore(ExtendedItem item, TextWriter writer);
}
