package net.arcadiusmc.factions;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.FieldPlacement;
import net.arcadiusmc.user.name.ProfileDisplayElement;

public class FactionProfileElement implements ProfileDisplayElement {

  @Override
  public void write(TextWriter writer, User user, DisplayContext context) {
    if (!context.profileViewable()) {
      return;
    }

    Faction faction = Factions.getManager().getCurrentFaction(user.getUniqueId());

    if (faction == null) {
      return;
    }

    writer.field(Messages.render("factions.profileField"), faction.displayName(context.viewer()));
  }

  @Override
  public FieldPlacement placement() {
    return FieldPlacement.ALL;
  }
}
