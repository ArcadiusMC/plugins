package net.arcadiusmc.core.listeners;

import com.google.common.base.Strings;
import java.util.UUID;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.JoinInfo;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.core.user.UserImpl;
import net.arcadiusmc.core.user.UserLookupImpl;
import net.arcadiusmc.core.user.UserLookupImpl.UserLookupEntry;
import net.arcadiusmc.core.user.UserServiceImpl;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.UserLookup.LookupEntry;
import net.arcadiusmc.user.event.UserJoinEvent;
import net.arcadiusmc.user.event.UserLogEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.slf4j.Logger;

class PlayerLoggingListener implements Listener {

  public static final Logger LOGGER = Loggers.getLogger();

  private final CorePlugin plugin;

  public PlayerLoggingListener(CorePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    UUID id = player.getUniqueId();
    String name = player.getName();

    UserServiceImpl service = plugin.getUserService();
    UserLookupImpl lookup = service.getLookup();

    UserLookupEntry entry;
    boolean firstJoin;

    if (!player.hasPlayedBefore()) {
      entry = lookup.createEntry(id, name);
      firstJoin = true;

      Location serverSpawn = ArcadiusServer.server().getServerSpawn();
      Location firstTimeSpawn = plugin.getCoreConfig().firstTimeSpawn();

      player.teleport(firstTimeSpawn);
      player.setBedSpawnLocation(serverSpawn, true);

      LOGGER.info("Player {} (uuid={}) joined for the first time", name, id);
    } else {
      entry = lookup.getEntry(id);
      firstJoin = false;

      if (entry == null) {
        LOGGER.warn(
            "Player {} or '{}' had no existing lookup entry, but has played before",
            id, name
        );

        entry = lookup.createEntry(id, name);
        firstJoin = true;
      }
    }

    lookup.changeIp(entry, player.getAddress().getAddress().getHostAddress());

    UserImpl user = service.getUser(entry);
    user.setOnline(true);
    user.setPlayer(player);

    String lastOnlineName = user.getLastOnlineName();
    boolean nameUpdated = updateOnlineName(user, name);

    if (Strings.isNullOrEmpty(lastOnlineName)) {
      lastOnlineName = name;
    }

    if (nameUpdated) {
      transferScores(lastOnlineName, name);
      lookup.onNameChange(entry, name);
    }

    event.joinMessage(null);

    if (firstJoin) {
      user.setTimeToNow(TimeField.FIRST_JOIN);
    }

    user.setTimeToNow(TimeField.LAST_LOGIN);

    user.updateTabName();
    user.updateFlying();
    user.updateGodMode();
    user.updateVanished();

    UserJoinEvent userEvent = new UserJoinEvent(user, lastOnlineName, firstJoin, false);
    userEvent.callEvent();
    UserLogEvent.maybeAnnounce(userEvent);

    JoinInfo joinInfo = plugin.getJoinInfo();
    joinInfo.show(user);

    plugin.getTabMenu().update();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UserServiceImpl service = plugin.getUserService();
    LookupEntry entry = service.getLookup().getEntry(player.getUniqueId());
    UserImpl user = service.getUser(entry);

    event.quitMessage(null);

    service.onUserLeave(user, event.getReason(), true);

    plugin.getTabMenu().update();
  }

  boolean updateOnlineName(UserImpl user, String name) {
    var lastOnlineName = user.getLastOnlineName();

    if (Strings.isNullOrEmpty(lastOnlineName)) {
      user.setLastOnlineName(name);
      return false;
    }

    if (lastOnlineName.equals(name)) {
      return false;
    }

    user.setLastOnlineName(name);
    user.getPreviousNames().add(name);

    return true;
  }

  void transferScores(String newName, String oldName) {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    for (var o: scoreboard.getObjectives()) {
      Score oldScore = o.getScore(oldName);
      Score newScore = o.getScore(newName);

      newScore.setScore(oldScore.getScore());
      oldScore.resetScore();
    }
  }
}
