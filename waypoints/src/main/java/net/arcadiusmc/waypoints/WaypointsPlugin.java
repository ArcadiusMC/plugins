package net.arcadiusmc.waypoints;

import net.arcadiusmc.FtcServer;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.packet.PacketListeners;
import net.arcadiusmc.packet.SignRenderer;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import net.arcadiusmc.waypoints.command.WaypointCommands;
import net.arcadiusmc.waypoints.listeners.WaypointsListeners;
import net.arcadiusmc.waypoints.type.WaypointTypes;
import org.bukkit.plugin.java.JavaPlugin;

public class WaypointsPlugin extends JavaPlugin {

  public WaypointConfig wConfig;

  private PeriodicalSaver saver;

  @Override
  public void onEnable() {
    reloadConfig();

    WaypointTypes.registerAll();
    WaypointManager.instance = new WaypointManager(this);

    SettingsBook<User> settingsBook = FtcServer.server().getGlobalSettingsBook();
    WaypointPrefs.createSettings(settingsBook);

    saver = PeriodicalSaver.create(
        WaypointManager.getInstance()::save,
        () -> wConfig.autoSaveInterval
    );

    saver.start();

    WaypointCommands.createCommands(WaypointManager.instance);
    WaypointsListeners.registerAll();

    Registry<SignRenderer> renderers = PacketListeners.listeners().getSignRenderers();
    renderers.register("waypoint_edit_sign", new WaypointSignRenderer(WaypointManager.instance));

    UserService service = Users.getService();
    service.setPropertyDefunct("homeWaypoint");
  }

  @Override
  public void onLoad() {
    WaypointWorldGuard.registerAll();
  }

  @Override
  public void onDisable() {
    saver.stop();

    var m = WaypointManager.getInstance();
    m.clear();

    Registry<SignRenderer> renderers = PacketListeners.listeners().getSignRenderers();
    renderers.remove("waypoint_edit_sign");
  }

  @Override
  public void reloadConfig() {
    wConfig = TomlConfigs.loadPluginConfig(this, WaypointConfig.class);
  }
}