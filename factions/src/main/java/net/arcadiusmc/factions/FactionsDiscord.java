package net.arcadiusmc.factions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.slf4j.Logger;

public class FactionsDiscord {

  public static final long NULL_ID = 0L;
  private static final Logger LOGGER = Loggers.getLogger();

  static void onLeave(Faction faction, User user) {
    Optional<Member> memberOpt = DiscordHook.getUserMember(user);

    if (memberOpt.isEmpty()) {
      return;
    }

    JDA jda = DiscordSRV.getPlugin().getJda();
    Member discMember = memberOpt.get();
    Guild guild = discMember.getGuild();

    long roleId = faction.get(Properties.ROLE_ID);
    Role role = jda.getRoleById(roleId);

    if (role != null) {
      guild.removeRoleFromMember(discMember, role)
          .submit()
          .whenComplete((unused, throwable) -> {
            if (throwable == null) {
              return;
            }

            LOGGER.error("Error removing role {} from player {} (Discord User={})",
                role.getName(), user.getName(), discMember.getEffectiveName(),
                throwable
            );
          });
    } else if (roleId != NULL_ID) {
      LOGGER.error("Unknown role {} for faction {}", roleId, faction.getKey());
    }

  }

  static void onJoin(Faction faction, User user) {
    Optional<Member> memberOpt = DiscordHook.getUserMember(user);

    if (memberOpt.isEmpty()) {
      user.sendMessage(Messages.renderText("factions.discord.link", user));
      return;
    }

    JDA jda = DiscordSRV.getPlugin().getJda();
    Member discMember = memberOpt.get();
    Guild guild = discMember.getGuild();

    long channelId = faction.get(Properties.CHANNEL_ID);
    TextChannel channel = jda.getTextChannelById(channelId);

    String key = faction.getKey();

    if (channel != null) {
      FactionsConfig config = Factions.getConfig();
      String template = config.getChannelUrlTemplate();

      String channelUrl = template
          .replace("%GUILD%", channel.getGuild().getId())
          .replace("%CHANNEL%", channel.getId());

      Component button = Messages.renderText("factions.discord.channelLink", user)
          .clickEvent(ClickEvent.openUrl(channelUrl));

      Component message = Messages.render("factions.discord.added")
          .addValue("channelLink", button)
          .create(user);

      user.sendMessage(message);
    } else if (channelId != NULL_ID) {
      LOGGER.error("Unknown text channel {} for faction {}", channelId, key);
    }

    long roleId = faction.get(Properties.ROLE_ID);
    Role role = jda.getRoleById(roleId);

    if (role != null) {
      guild.addRoleToMember(discMember, role)
          .submit()
          .whenComplete((unused, throwable) -> {
            if (throwable == null) {
              return;
            }

            LOGGER.error("Error giving player {} (Discord User={}) {} role",
                user.getName(), discMember.getEffectiveName(), role.getName(),
                throwable
            );
          });
    } else if (roleId != NULL_ID) {
      LOGGER.error("Unknown role {} for faction {}", roleId, key);
    }
  }
}
