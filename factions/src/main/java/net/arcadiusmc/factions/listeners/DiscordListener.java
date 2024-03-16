package net.arcadiusmc.factions.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import java.util.UUID;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionManager;
import net.arcadiusmc.factions.FactionsDiscord;
import net.arcadiusmc.user.Users;

class DiscordListener {

  private final FactionManager manager;

  public DiscordListener(FactionManager manager) {
    this.manager = manager;
  }

  @Subscribe
  public void onAccountLinked(AccountLinkedEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    User user = event.getUser();

    Guild guild = DiscordSRV.getPlugin().getMainGuild();
    if (guild == null) {
      // No idea how this could happen but you never know
      return;
    }

    Faction faction = manager.getCurrentFaction(playerId);
    if (faction == null) {
      return;
    }

    Member member = guild.getMember(user);
    if (member == null) {
      return;
    }

    FactionsDiscord.giveFactionBenefits(faction, Users.get(playerId), member);
  }

  @Subscribe
  public void onAccountUnlinked(AccountUnlinkedEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    User user = event.getDiscordUser();

    Guild guild = DiscordSRV.getPlugin().getMainGuild();
    if (guild == null) {
      return;
    }

    Faction faction = manager.getCurrentFaction(playerId);
    if (faction == null) {
      return;
    }

    Member member = guild.getMember(user);
    if (member == null) {
      return;
    }

    FactionsDiscord.removeBenefits(faction, Users.get(playerId), member);
  }
}
