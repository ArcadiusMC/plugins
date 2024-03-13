package net.arcadiusmc.items.wreath;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Getter @Setter
public class OwnerLore extends ItemComponent implements LoreElement {

  private static final Logger LOGGER = Loggers.getLogger();

  static final int MAX_NAME_LENGTH = TextInfo.getPxWidth("<longassnamebruh>");

  private Component title;

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    UUID ownerId = item.getComponent(Owner.class).map(Owner::getPlayerId).orElse(null);

    if (ownerId == null) {
      return;
    }

    Component title;
    if (Text.isEmpty(this.title)) {
      title = Messages.renderText("itemsPlugin.wreath.defaultTitle");
    } else {
      title = this.title;
    }

    User user = Users.get(ownerId);
    String name = user.getName();

    int nameLength = TextInfo.getPxWidth(name);

    String messageKey = "itemsPlugin.wreath.bestowedTo."
        + (nameLength > MAX_NAME_LENGTH ? "multi" : "single");

    Component lore = Messages.render(messageKey)
        .addValue("name", name)
        .addValue("title", title)
        .asComponent();

    writer.line(lore);
  }

  @Override
  public void save(CompoundTag tag) {
    if (Text.isEmpty(title)) {
      return;
    }

    ExtraCodecs.COMPONENT.encodeStart(TagOps.OPS, title)
        .mapError(s -> "Failed to save wreath owner title: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(t -> tag.put("owner_title", t));
  }

  @Override
  public void load(CompoundTag tag) {
    if (!tag.containsKey("owner_title")) {
      this.title = null;
      return;
    }

    ExtraCodecs.COMPONENT.parse(TagOps.OPS, tag.get("owner_title"))
        .mapError(s -> "Failed to load wreath owner title: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(component -> this.title = component);
  }
}
