package net.arcadiusmc.factions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.audience.Audience;

public interface FExceptions {

  static CommandSyntaxException notInFaction(Audience viewer, User user) {
    return Messages.render("factions.errors.notInFaction")
        .addValue("player", user)
        .exception(viewer);
  }
}
