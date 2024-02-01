package net.arcadiusmc.titles;

import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.FieldPlacement;
import net.arcadiusmc.user.name.ProfileDisplayElement;

public class TitleProfileElement implements ProfileDisplayElement {

  @Override
  public void write(TextWriter writer, User user, DisplayContext context) {
    UserTitles component = user.getComponent(UserTitles.class);
    UserRank rank = component.getTitle();
    RankTier tier = component.getTier();

    if (tier.ordinal() >= RankTier.TIER_1.ordinal()) {
      writer.field("Tier", tier.getDisplayName());
    }

    if (rank != UserRanks.DEFAULT) {
      writer.field("Rank", rank.asComponent());
    }
  }

  @Override
  public FieldPlacement placement() {
    return FieldPlacement.IN_PROFILE;
  }
}
