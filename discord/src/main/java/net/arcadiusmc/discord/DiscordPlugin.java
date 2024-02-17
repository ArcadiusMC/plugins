package net.arcadiusmc.discord;

import com.mojang.serialization.JsonOps;
import github.scarsz.discordsrv.DiscordSRV;
import java.nio.file.Path;
import lombok.Getter;
import net.arcadiusmc.discord.commands.AppenderCommand;
import net.arcadiusmc.discord.listener.AnnouncementForwardingListener;
import net.arcadiusmc.discord.listener.ServerLoadListener;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.TomlConfigs;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordPlugin extends JavaPlugin {

  @Getter
  private Config pluginConfig = new Config();

  @Getter
  private BoostCommands boostCommands = BoostCommands.EMPTY;

  @Override
  public void onEnable() {
    if (!PluginUtil.isEnabled("DiscordSRV")) {
      getSLF4JLogger().error("DiscordSRV not found, disabling self...");
      getServer().getPluginManager().disablePlugin(this);

      return;
    }

    reloadConfig();

    new AppenderCommand();

    Events.register(new ServerLoadListener(this));
    DiscordSRV.api.subscribe(new AnnouncementForwardingListener(this));

    var nameFactory = Users.getService().getNameFactory();
    nameFactory.addProfileField("discord_id", 36, new DiscordProfileField());
  }

  @Override
  public void reloadConfig() {
    pluginConfig = TomlConfigs.loadPluginConfig(this, Config.class);
    updateLoggers(false);

    Path commandFile = PathUtil.pluginPath("boost-commands.yml");
    PluginJar.saveResources("boost-commands.yml", commandFile);

    SerializationHelper.readAsJson(commandFile, jsonObject -> {
      BoostCommands.CODEC.parse(JsonOps.INSTANCE, jsonObject)
          .mapError(s -> "Failed to load " + commandFile + ": " + s)
          .resultOrPartial(getSLF4JLogger()::error)
          .ifPresent(commands -> boostCommands = commands);
    });
  }

  @Override
  public void onDisable() {
    updateLoggers(true);
    Users.getService().getNameFactory().removeField("discord_id");
  }

  void updateLoggers(boolean remove) {
    var ctx = LoggerContext.getContext(false);
    var config = ctx.getConfiguration();
    var root = config.getRootLogger();

    root.removeAppender(DiscordAppender.APPENDER_NAME);

    if (!remove) {
      root.addAppender(new DiscordAppender(this.pluginConfig), getAppenderLevel(), null);
    }

    LoggerConfig discordSRV = new LoggerConfig("DiscordSRV", Level.OFF, false);
    config.addLogger("DiscordSRV", discordSRV);

    ctx.updateLoggers();
  }

  Level getAppenderLevel() {
    String name = pluginConfig.forwarderLevel();
    return Level.toLevel(name, Level.ERROR);
  }
}