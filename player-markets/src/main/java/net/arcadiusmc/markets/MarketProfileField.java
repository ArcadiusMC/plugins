package net.arcadiusmc.markets;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.FieldPlacement;
import net.arcadiusmc.user.name.ProfileDisplayElement;

public class MarketProfileField implements ProfileDisplayElement {

  private final MarketsPlugin plugin;

  public MarketProfileField(MarketsPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void write(TextWriter writer, User user, DisplayContext context) {
    Market owned = plugin.getManager().getByOwner(user.getUniqueId());
    if (owned == null) {
      return;
    }

    writer.field(
        Messages.renderText("markets.profileField", context.viewer()),
        owned.displayName()
    );
  }

  @Override
  public FieldPlacement placement() {
    return FieldPlacement.IN_PROFILE;
  }
}
