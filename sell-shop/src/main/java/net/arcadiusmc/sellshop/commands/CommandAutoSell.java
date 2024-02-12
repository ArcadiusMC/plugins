package net.arcadiusmc.sellshop.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.sellshop.UserShopData;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

@CommandFile("commands/auto_sell.gcn")
public class CommandAutoSell {

  void listOther(CommandSource source, @Argument("player") User user)
      throws CommandSyntaxException
  {
    UserShopData data = user.getComponent(UserShopData.class);

    if (data.getAutoSelling().isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(user);
    }

    source.sendMessage(
        Messages.render("cmd.autosell.list")
            .addValue("player", user)
            .addValue("list",
                TextJoiner.onComma()
                    .add(data.getAutoSelling()
                        .stream()
                        .map(material -> Component.text(material.name().toLowerCase()))
                    )
                    .asComponent()
            )
            .create(source)
    );
  }

  void add(
      CommandSource source,
      @Argument("player") User user,
      @Argument("material") Material material
  ) {
    var earnings = user.getComponent(UserShopData.class);
    earnings.getAutoSelling().add(material);

    source.sendSuccess(
        Messages.render("cmd.autosell.added")
            .addValue("player", user)
            .addValue("material", material)
            .create(source)
    );
  }

  void remove(
      CommandSource source,
      @Argument("player") User user,
      @Argument("material") Material material
  ) {
    var earnings = user.getComponent(UserShopData.class);
    earnings.getAutoSelling().remove(material);

    source.sendSuccess(
        Messages.render("cmd.autosell.removed")
            .addValue("player", user)
            .addValue("material", material)
            .create(source)
    );
  }

  void clear(CommandSource source, @Argument("player") User user) {
    UserShopData data = user.getComponent(UserShopData.class);
    data.getAutoSelling().clear();

    source.sendSuccess(
        Messages.render("cmd.autosell.cleared")
            .addValue("player", user)
            .create(source)
    );
  }
}
