package net.arcadiusmc.markets;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.audience.Audience;

public interface MExceptions {

  static CommandSyntaxException ownsNoMarket(Audience viewer, User user) {
    return Messages.render("markets.errors.ownsNoMarket")
        .addValue("player", user)
        .exception(viewer);
  }

  static CommandSyntaxException unknownMarket(String name, StringReader reader) {
    return Messages.render("markets.errors.unknownMarket")
        .addValue("name", name)
        .exceptionWithContext(reader);
  }

  static CommandSyntaxException noMarketOwned(Audience viewer) {
    return Messages.render("markets.errors.noMarketOwned")
        .exception(viewer);
  }

  static CommandSyntaxException notMerged(Audience viewer) {
    return Messages.render("markets.errors.notMerged").exception(viewer);
  }
}
