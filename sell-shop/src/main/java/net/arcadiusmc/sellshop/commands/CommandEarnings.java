package net.arcadiusmc.sellshop.commands;

import static net.arcadiusmc.text.Text.format;

import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.sellshop.UserShopData;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

@CommandData("file = commands/earnings.gcn")
public class CommandEarnings {

  void getEarnings(CommandSource source, @Argument("player") User user)
      throws CommandSyntaxException
  {
    var earnings = user.getComponent(UserShopData.class);

    if (earnings.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(user);
    }

    source.sendSuccess(
        Messages.render("cmd.earnings.list")
            .addValue("player", user)
            .addValue("list",
                TextJoiner.onNewLine()
                    .add(
                        Streams.stream(earnings)
                            .map(entry -> format("&7{0}&8:&r {1, rhines}",
                                entry.getMaterial(),
                                entry.getValue()
                            ))
                    )
            )
            .create(source)
    );
  }

  Component message(
      CommandSource source,
      String suffix,
      User user,
      Material material,
      int amount,
      int newAmount
  ) {
    return Messages.render("cmd.earnings", suffix)
        .addValue("material", material)
        .addValue("player", user)
        .addValue("value", amount)
        .addValue("newValue", newAmount)
        .create(source);
  }

  void clearEarnings(CommandSource source, @Argument("player") User user) {
    var earnings = user.getComponent(UserShopData.class);
    earnings.clear();

    source.sendSuccess(
        Messages.render("cmd.earnings.cleared.all")
            .addValue("player", user)
            .create(source)
    );
  }

  void addEarnings(
      CommandSource source,
      @Argument("player") User user,
      @Argument("amount") int amount,
      @Argument("material") Material material
  ) {
    var earnings = user.getComponent(UserShopData.class);
    int newAmount = earnings.get(material) + amount;

    earnings.set(material, newAmount);

    source.sendSuccess(message(source, "added", user, material, amount, newAmount));
  }

  void removeEarnings(
      CommandSource source,
      @Argument("player") User user,
      @Argument("amount") int amount,
      @Argument("material") Material material
  ) throws CommandSyntaxException {
    var earnings = user.getComponent(UserShopData.class);
    int newAmount = earnings.get(material) - amount;

    earnings.set(material, newAmount);

    source.sendSuccess(message(source, "removed", user, material, amount, newAmount));
  }

  void setEarnings(
      CommandSource source,
      @Argument("player") User user,
      @Argument("amount") int amount,
      @Argument("material") Material material
  ) throws CommandSyntaxException {
    var earnings = user.getComponent(UserShopData.class);
    earnings.set(material, amount);

    if (amount == 0) {
      source.sendSuccess(message(source, "cleared", user, material, 0, 0));
    } else {
      source.sendSuccess(message(source, "set", user, material, amount, amount));
    }
  }

  Component removedMessage(Material material, User user) {
    return format("Removed {0} earnings from {1, user}",
        material.name().toLowerCase(), user
    );
  }
}
