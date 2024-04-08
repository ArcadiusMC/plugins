package net.arcadiusmc.waypoints;

import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.packet.PacketListeners;
import net.arcadiusmc.packet.SignRenderer;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
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

  private final MessageList messageList = MessageList.create();

  public WaypointConfig wConfig;

  private PeriodicalSaver saver;

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    reloadConfig();

    WaypointTypes.registerAll();
    WaypointManager.instance = new WaypointManager(this);

    SettingsBook<User> settingsBook = ArcadiusServer.server().getGlobalSettingsBook();
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

    WaypointManager.instance.discoverer.beginListening();
  }

  @Override
  public void onLoad() {
    WaypointWorldGuard.registerAll();
  }

  @Override
  public void onDisable() {
    saver.stop();

    WaypointManager m = WaypointManager.getInstance();
    m.discoverer.stopListening();
    m.save();
    m.clear();

    Registry<SignRenderer> renderers = PacketListeners.listeners().getSignRenderers();
    renderers.remove("waypoint_edit_sign");

    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    wConfig = TomlConfigs.loadPluginConfig(this, WaypointConfig.class);
    MessageLoader.loadPluginMessages(this, messageList);
  }
}
