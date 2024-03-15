package net.arcadiusmc.factions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.JDA.Status;
import net.arcadiusmc.utils.PluginUtil;

public class Factions {

  public static FactionsPlugin getPlugin() {
    return FactionsPlugin.plugin();
  }

  public static FactionsConfig getConfig() {
    return getPlugin().getPluginConfig();
  }

  public static FactionManager getManager() {
    return getPlugin().getManager();
  }

  public static boolean isDiscordEnabled() {
    if (!PluginUtil.isEnabled("DiscordSRV")) {
      return false;
    }

    JDA jda = DiscordSRV.getPlugin().getJda();

    if (jda == null) {
      return false;
    }

    return jda.getStatus() == Status.CONNECTED;
  }
}
