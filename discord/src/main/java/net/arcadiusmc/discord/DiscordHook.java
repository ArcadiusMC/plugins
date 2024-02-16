package net.arcadiusmc.discord;

import com.google.common.base.Strings;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.PluginUtil;

/**
 * Class for interacting with the Discord server using the discord srv plugin
 */
public final class DiscordHook {
  private DiscordHook() {}

  public static boolean isActive() {
    if (!PluginUtil.isEnabled("DiscordSRV")) {
      return false;
    }

    return DiscordSRV.getPlugin().getJda() != null;
  }

  public static UUID getPlayerId(String discordId) {
    Objects.requireNonNull(discordId);

    return DiscordSRV.getPlugin()
        .getAccountLinkManager()
        .getUuid(discordId);
  }

  public static Optional<TextChannel> findChannel(String name) {
    return Optional.ofNullable(
        DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(name)
    );
  }

  public static UUID getPlayerId(Member member) {
    return getPlayerId(member.getId());
  }

  public static Optional<Member> getUserMember(User user) {
    return getUserMember(user.getUniqueId());
  }

  public static Optional<Member> getUserMember(UUID playerId) {
    Objects.requireNonNull(playerId);

    AccountLinkManager links = DiscordSRV.getPlugin().getAccountLinkManager();
    String discordId = links.getDiscordId(playerId);

    if (Strings.isNullOrEmpty(discordId)) {
      return Optional.empty();
    }

    Guild guild = DiscordSRV.getPlugin().getMainGuild();
    Member member = guild.getMemberById(discordId);

    return Optional.ofNullable(member);
  }
}