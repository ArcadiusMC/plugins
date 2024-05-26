package net.arcadiusmc.voicechat;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import lombok.Getter;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.io.ConfigCodec;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class HookPlugin extends JavaPlugin {

  private Config hookConfig;

  @Override
  public void onEnable() {
    if (!PluginUtil.isEnabled("voicechat")) {
      getSLF4JLogger().error("No voicechat plugin found, disabling self");

      PluginManager pl = getServer().getPluginManager();
      pl.disablePlugin(this);

      return;
    }

    reloadConfig();

    BukkitVoicechatService service = BukkitServices.loadOrThrow(BukkitVoicechatService.class);
    service.registerPlugin(new HookVcPlugin(this));

    new HookVcCommand(this);
  }

  @Override
  public void reloadConfig() {
    ConfigCodec.loadPluginConfig(this, Config.CODEC)
        .ifPresentOrElse(
            config -> hookConfig = config,
            () -> hookConfig = Config.DEFAULT
        );
  }

  @Override
  public void onDisable() {

  }
}
