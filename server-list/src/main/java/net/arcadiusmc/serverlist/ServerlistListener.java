package net.arcadiusmc.serverlist;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.common.base.Strings;
import java.util.Random;
import net.arcadiusmc.events.DayChangeEvent;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserLookup;
import net.arcadiusmc.user.UserLookup.LookupEntry;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerlistListener implements Listener {

  private final ServerlistPlugin plugin;

  public ServerlistListener(ServerlistPlugin plugin) {
    this.plugin = plugin;
  }

  void logPing(PaperServerListPingEvent event) {
    var logger = plugin.getSLF4JLogger();
    if (!logger.isDebugEnabled()) {
      return;
    }

    String address = event.getAddress().getHostAddress();

    UserService users = Users.getService();
    LookupEntry profile = users.getLookup().query(address);

    if (profile == null) {
      logger.debug("Received server ping from IP {}", address);
    } else {
      logger.debug("Received server ping from player {} (IP={})", profile.getName(), address);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPaperServerListPing(PaperServerListPingEvent event) {
    logPing(event);

    ServerListDisplay display = plugin.getDisplay();
    var config = plugin.getListConfig();

    if (config.appearOffline()) {
      event.setCancelled(true);
      return;
    }

    int playerCountRange = config.maxPlayerRandomRange();

    if (playerCountRange > 0) {
      Random random = display.getRandom();
      int max = Bukkit.getMaxPlayers();
      int newMax = random.nextInt(max, max + playerCountRange);
      event.setMaxPlayers(newMax);
    }

    ListDisplayData pair = display.getCurrent();
    var base = config.baseMotd() == null ? null : Text.valueOf(config.baseMotd());

    if (base == null) {
      base = Bukkit.motd();
    }

    PlaceholderRenderer placeholders = Placeholders.newRenderer()
        .useDefaults()
        .add("version", Bukkit::getMinecraftVersion)
        .add("ip", event.getAddress().getHostAddress());

    UserService service = Users.getService();
    UserLookup lookup = service.getLookup();

    LookupEntry entry = lookup.query(event.getAddress().getHostAddress());

    User user;

    if (entry != null && config.inferPlayerBasedOffIp() && service.userLoadingAllowed()) {
      user = service.getUser(entry);
    } else {
      user = null;
    }

    placeholders.add("message", placeholders.render(pair.motdPart, user));
    event.motd(placeholders.render(base, user));

    if (pair.icon != null) {
      event.setServerIcon(pair.icon);
    }

    if (pair.protocolOverride > 0 && config.allowChangingProtocolVersions()) {
      event.setProtocolVersion(pair.protocolOverride);
    }

    if (!Strings.isNullOrEmpty(pair.versionText) && config.allowChangingVersionText()) {
      String text = pair.versionText;
      Component versionBase = placeholders.render(Text.valueOf(text, user), user);
      event.setVersion(Text.SECTION_LEGACY.serialize(versionBase));
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDayChange(DayChangeEvent event) {
    ServerListDisplay display = JavaPlugin.getPlugin(ServerlistPlugin.class).getDisplay();
    display.cacheDateEntries();
  }
}
