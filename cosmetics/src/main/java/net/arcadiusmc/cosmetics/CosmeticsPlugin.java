package net.arcadiusmc.cosmetics;

import lombok.Getter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.cosmetics.command.CommandCosmetics;
import net.arcadiusmc.cosmetics.listeners.CosmeticListeners;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.io.ConfigCodec;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class CosmeticsPlugin extends JavaPlugin {

  private ActiveMap activeMap;
  private CosmeticsConfig cosmeticsConfig = CosmeticsConfig.DEFAULT;
  private CosmeticMenus menus;

  public static CosmeticsPlugin plugin() {
    return getPlugin(CosmeticsPlugin.class);
  }

  @Override
  public void onEnable() {
    activeMap = new ActiveMap(getDataPath().resolve("active.json"));

    reloadConfig();
    activeMap.load();

    Cosmetics.registerAll(cosmeticsConfig);
    menus = new CosmeticMenus();

    ArcadiusServer server = ArcadiusServer.server();
    CosmeticsSettings.registerAll(server.getGlobalSettingsBook());

    new CommandCosmetics(this);
    CosmeticListeners.registerAll(this);
  }

  @Override
  public void onDisable() {
    activeMap.save();
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this);

    cosmeticsConfig = ConfigCodec.loadPluginConfig(this, CosmeticsConfig.CODEC)
        .orElse(CosmeticsConfig.DEFAULT);
  }
}
