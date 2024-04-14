package net.arcadiusmc.factions;

import com.google.common.base.Strings;
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

  private static final Logger LOGGER = Loggers.getLogger();

  static void onLeave(Faction faction, User user) {
    Optional<Member> memberOpt = DiscordHook.getUserMember(user);

    if (memberOpt.isEmpty()) {
      return;
    }

    removeBenefits(faction, user, memberOpt.get());
  }

  static void onJoin(Faction faction, User user) {
    Optional<Member> memberOpt = DiscordHook.getUserMember(user);

    if (memberOpt.isEmpty()) {
      user.sendMessage(Messages.renderText("factions.discord.link", user));
      return;
    }

    Member discMember = memberOpt.get();
    giveFactionBenefits(faction, user, discMember);
  }

  private static Optional<String> optionalId(String str) {
    return Optional.ofNullable(Strings.emptyToNull(str));
  }

  public static void giveFactionBenefits(Faction faction, User user, Member discMember) {
    JDA jda = DiscordSRV.getPlugin().getJda();
    Guild guild = discMember.getGuild();

    String channelId = faction.get(Properties.CHANNEL_ID);
    TextChannel channel = optionalId(channelId).map(jda::getTextChannelById).orElse(null);

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
    } else if (channelId != null) {
      LOGGER.error("Unknown text channel {} for faction {}", channelId, key);
    }

    String roleId = faction.get(Properties.ROLE_ID);
    Role role = optionalId(roleId).map(jda::getRoleById).orElse(null);

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
    } else if (roleId != null) {
      LOGGER.error("Unknown role {} for faction {}", roleId, key);
    }
  }

  public static void removeBenefits(Faction faction, User user, Member discMember) {
    JDA jda = DiscordSRV.getPlugin().getJda();
    Guild guild = discMember.getGuild();

    String roleId = faction.get(Properties.ROLE_ID);
    Role role = optionalId(roleId).map(jda::getRoleById).orElse(null);

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
    } else if (roleId != null) {
      LOGGER.error("Unknown role {} for faction {}", roleId, faction.getKey());
    }
  }
}
