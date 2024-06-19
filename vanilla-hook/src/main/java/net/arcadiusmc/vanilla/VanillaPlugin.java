package net.arcadiusmc.vanilla;

import net.arcadiusmc.vanilla.listeners.InjectionListeners;
import net.arcadiusmc.packet.PacketListeners;
import net.arcadiusmc.vanilla.packet.ListenersImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VanillaPlugin extends JavaPlugin {

  static final int CURRENT_DATA_VERSION = 3953;

  @Override
  public void onEnable() {
    // Ensure the plugin's set vanilla version is the
    // same as the server's version
    int serverDataVersion = Bukkit.getUnsafe().getDataVersion();
    if (serverDataVersion != CURRENT_DATA_VERSION) {
      throw new IllegalStateException(String.format(
          "Minecraft DataVersion mismatch for vanilla-hook plugin! Versions: server=%s plugin=%s",
          serverDataVersion, CURRENT_DATA_VERSION
      ));
    }

    ListenersImpl listeners = ListenersImpl.getListeners();
    listeners.initialize();

    ServicesManager services = Bukkit.getServicesManager();
    services.register(PacketListeners.class, listeners, this, ServicePriority.Normal);

    PluginManager pl = Bukkit.getPluginManager();
    pl.registerEvents(new InjectionListeners(listeners), this);

    DefaultRenderers.registerAll(listeners);
  }

  @Override
  public void onDisable() {
    ListenersImpl.getListeners().shutdown();
  }
}
