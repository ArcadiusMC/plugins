package net.arcadiusmc.titles.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.titles.RankMenu;
import net.arcadiusmc.titles.Tier;
import net.arcadiusmc.titles.Tiers;
import net.arcadiusmc.titles.Title;
import net.arcadiusmc.titles.Titles;
import net.arcadiusmc.titles.TitlesPlugin;
import net.arcadiusmc.titles.UserTitles;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandData;
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ArrayArgument;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.plugin.java.JavaPlugin;

@CommandData("file = titles_command.gcn")
public class TitlesCommand {

  static final String ADMIN_PERMISSION = "ftc.commands.ranks.admin";

  static final String ARG = "user";
  static final String TITLE = "title";
  static final String TITLES = "titles";
  static final String TIER = "tier";

  @VariableInitializer
  void createVars(Map<String, Object> map) {
    map.put("tier",
        ArgumentTypes.map(
            new RegistryArguments<>(Tiers.REGISTRY, "Tier"),
            Holder::getValue
        )
    );

    ArgumentType<Holder<Title>> rankType = new RegistryArguments<>(Titles.REGISTRY, "Rank");

    ArgumentType<Title> mapped = ArgumentTypes.map(rankType, Holder::getValue);
    ArrayArgument<Title> array = ArgumentTypes.array(mapped);

    map.put("title", mapped);
    map.put("titles", array);
  }

  void openMenu(CommandSource source) throws CommandSyntaxException {
    User user = Commands.getUserSender(source);
    RankMenu.getInstance().open(user);
  }

  void reloadPlugin(CommandSource source) {
    TitlesPlugin plugin = JavaPlugin.getPlugin(TitlesPlugin.class);

    plugin.reloadConfig();
    plugin.load();

    source.sendSuccess(Messages.renderText("cmd.ranks.reloaded", source));
  }

  void showTitlesInfo(CommandSource source, @Argument(ARG) User user) {
    UserTitles titles = user.getComponent(UserTitles.class);

    var writer = TextWriters.newWriter();
    writer.setFieldValueStyle(Style.style(NamedTextColor.GRAY));

    writer.formatted("{0, user}'s rank tier and titles:", user);
    writer.field("Tier", titles.getTier().getDisplayName());

    Title title = titles.getTitle();
    if (title != Titles.DEFAULT) {
      writer.field("Title", title);
    }

    Set<Title> available = titles.getAvailable();
    if (!available.isEmpty()) {
      writer.field("Available", TextJoiner.onComma().add(available));
    }

    source.sendMessage(writer.asComponent());
  }

  void setTitle(CommandSource source, @Argument(ARG) User user, @Argument(TITLE) Title rank)
      throws CommandSyntaxException
  {
    UserTitles titles = user.getComponent(UserTitles.class);

    if (titles.getTitle().equals(rank)) {
      throw Messages.render("cmd.ranks.error.titleAlreadySet")
          .addValue("player", user)
          .addValue("title", rank.asComponent())
          .exception(source);
    }

    titles.setTitle(rank);

    source.sendSuccess(
        Messages.render("cmd.ranks.set")
            .addValue("player", user)
            .addValue("title", rank.asComponent())
            .create(source)
    );
  }

  void addTitles(
      CommandSource source,
      @Argument(ARG) User user,
      @Argument(TITLES) Collection<Title> ranks
  ) throws CommandSyntaxException {

    UserTitles titles = user.getComponent(UserTitles.class);

    for (Title rank : ranks) {
      ensureNotDefault(rank);

      if (titles.hasTitle(rank)) {
        throw Messages.render("cmd.ranks.error.alreadyOwned")
            .addValue("player", user)
            .addValue("title", rank.asComponent())
            .exception(source);
      }
    }

    ranks.forEach(titles::addTitle);

    if (ranks.size() == 1) {
      source.sendSuccess(
          Messages.render("cmd.ranks.added.single")
              .addValue("player", user)
              .addValue("title", ranks.iterator().next().asComponent())
              .create(source)
      );
    } else {
      source.sendSuccess(
          Messages.render("cmd.ranks.added.multiple")
              .addValue("player", user)
              .addValue("titles", ranks.size())
              .create(source)
      );
    }
  }

  void removeTitles(
      CommandSource source,
      @Argument(ARG) User user,
      @Argument(TITLES) Collection<Title> ranks
  ) throws CommandSyntaxException {
    UserTitles titles = user.getComponent(UserTitles.class);

    for (Title rank : ranks) {
      ensureNotDefault(rank);

      if (!titles.hasTitle(rank)) {
        throw Messages.render("cmd.ranks.error.notOwned")
            .addValue("player", user)
            .addValue("title", rank.asComponent())
            .exception(source);
      }
    }

    ranks.forEach(titles::removeTitle);

    if (ranks.size() == 1) {
      source.sendSuccess(
          Messages.render("cmd.ranks.removed.single")
              .addValue("player", user)
              .addValue("title", ranks.iterator().next().asComponent())
              .create(source)
      );
    } else {
      source.sendSuccess(
          Messages.render("cmd.ranks.removed.multiple")
              .addValue("player", user)
              .addValue("titles", ranks.size())
              .create(source)
      );
    }
  }

  void setTier(CommandSource source, @Argument(ARG) User user, @Argument(TIER) Tier tier)
      throws CommandSyntaxException
  {
    UserTitles titles = user.getComponent(UserTitles.class);

    if (titles.getTier() == tier) {
      throw Messages.render("cmd.ranks.error.tierAlreadySet")
          .addValue("tier", tier.displayName())
          .addValue("player", user)
          .exception(source);
    }

    titles.setTier(tier);

    source.sendSuccess(
        Messages.render("cmd.ranks.tier.set")
            .addValue("tier", tier.displayName())
            .addValue("player", user.displayName())
            .create(source)
    );
  }

  static void ensureNotDefault(Title rank) throws CommandSyntaxException {
    if (!rank.isDefaultTitle()) {
      return;
    }

    throw Messages.render("cmd.ranks.error.defaultTitle")
        .addValue("title", rank.asComponent())
        .exception();
  }
}