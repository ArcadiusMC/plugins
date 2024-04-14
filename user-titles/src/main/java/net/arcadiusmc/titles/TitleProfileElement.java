package net.arcadiusmc.titles;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.FieldPlacement;
import net.arcadiusmc.user.name.ProfileDisplayElement;

public class TitleProfileElement implements ProfileDisplayElement {

  @Override
  public void write(TextWriter writer, User user, DisplayContext context) {
    UserTitles component = UserTitles.load(user);
    Holder<Title> rank = component.getActive();
    Holder<Tier> tier = component.getTier();

    if (tier != Tiers.DEFAULT_HOLDER) {
      writer.field("Tier", tier.getValue().displayName());
    }

    if (rank != Titles.DEFAULT_HOLDER) {
      writer.field("Rank", rank.getValue().asComponent());
    }
  }

  @Override
  public FieldPlacement placement() {
    return FieldPlacement.IN_PROFILE;
  }
}
