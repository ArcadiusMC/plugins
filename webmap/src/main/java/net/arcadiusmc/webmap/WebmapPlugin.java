package net.arcadiusmc.webmap;

import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.webmap.bluemap.BlueWebmap;
import net.arcadiusmc.webmap.dynmap.DynmapWebmap;
import net.arcadiusmc.webmap.listeners.GameModeListener;
import org.bukkit.plugin.java.JavaPlugin;

public class WebmapPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  private WebMap implementation;

  @Override
  public void onEnable() {
    boolean dynmap = PluginUtil.isEnabled("dynmap");
    boolean blumap = PluginUtil.isEnabled("BlueMap");

    if (blumap && dynmap) {
      getSLF4JLogger().warn(
          "Both BlueMap and Dynmap found, only BlueMap will be kept in sync "
              + "with plugin-induced map changes"
      );
    }

    if (blumap) {
      implementation = new BlueWebmap(getDataFolder().toPath());
    } else if (dynmap) {
      implementation = new DynmapWebmap();
    } else {
      getSLF4JLogger().error("No Dynmap or BlueMap plugin found... disabling self");

      var pl = getServer().getPluginManager();
      pl.disablePlugin(this);

      return;
    }

    BukkitServices.register(WebMap.class, implementation);
    HideSetting.createSetting(ArcadiusServer.server().getGlobalSettingsBook());
    Events.register(new GameModeListener());

    MessageLoader.loadPluginMessages(this, messageList);
  }

  @Override
  public void onDisable() {
    if (implementation instanceof BlueWebmap webmap) {
      webmap.save();
    }
  }
}
