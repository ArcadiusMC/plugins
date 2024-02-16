package net.arcadiusmc.discord.listener;

import github.scarsz.discordsrv.dependencies.jda.api.events.GenericEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.EventListener;
import java.util.List;
import java.util.UUID;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.discord.BoostCommands;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.discord.DiscordPlugin;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class BoostListener implements EventListener {

  public static final Logger LOGGER = Loggers.getLogger();
  private final DiscordPlugin plugin;

  public BoostListener(DiscordPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void onEvent(@NotNull GenericEvent genericEvent) {
    if (!(genericEvent instanceof GuildMemberUpdateBoostTimeEvent event)) {
      return;
    }

    onBoostEvent(event);
  }

  private void onBoostEvent(GuildMemberUpdateBoostTimeEvent event) {
    boolean boosting = isBoosting(event);
    UUID uuid = DiscordHook.getPlayerId(event.getMember());

    if (uuid == null) {
      LOGGER.warn(
          "Cannot execute boost commands for user {}... no linked player account found",
          event.getMember().getEffectiveName()
      );

      return;
    }

    User user = Users.get(uuid);
    BoostCommands commands = plugin.getBoostCommands();
    CommandSource source = Grenadier.createSource(Bukkit.getConsoleSender()).silent();

    if (boosting && !commands.boostingBegin().isEmpty()) {
      execList(commands.boostingBegin(), user, source);
    } else if (!commands.boostingEnd().isEmpty()) {
      execList(commands.boostingEnd(), user, source);
    }
  }

  void execList(List<String> commands, User user, CommandSource source) {
    for (String s : commands) {
      String formatted = Commands.replacePlaceholders(s, user);
      Commands.execute(source, formatted);
    }
  }

  public static boolean isBoosting(GuildMemberUpdateBoostTimeEvent event) {
    if (event.getOldTimeBoosted() == null) {
      return true;
    }

    if (event.getNewTimeBoosted() == null) {
      return false;
    }

    return event.getOldTimeBoosted().isBefore(event.getNewTimeBoosted());
  }
}
