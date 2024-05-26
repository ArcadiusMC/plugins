package net.arcadiusmc.items.tools;

import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.wreath.OwnerLore;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;

public enum CraftedForLore implements LoreElement {
  ELEMENT;

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    Optional<UUID> opt = item.getComponent(Owner.class).map(Owner::getPlayerId);

    if (opt.isEmpty()) {
      return;
    }

    UUID ownerId = opt.get();
    User user = Users.get(ownerId);

    String name = user.getName();

    int pxWidth = TextInfo.getPxWidth(name);
    String messageKeySuffix;

    if (pxWidth > OwnerLore.MAX_NAME_LENGTH) {
      messageKeySuffix = "multi";
    } else {
      messageKeySuffix = "single";
    }

    Component lore = Messages.render("itemsPlugin.tools.craftedFor", messageKeySuffix)
        .addValue("name", name)
        .asComponent();

    writer.line(lore);
  }
}
