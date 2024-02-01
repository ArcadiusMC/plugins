package net.arcadiusmc.titles;

import java.nio.file.Path;
import lombok.Getter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.titles.commands.TitlesCommand;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.UserNameFactory;
import net.arcadiusmc.utils.TomlConfigs;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class TitlesPlugin extends JavaPlugin {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private TitlesConfig titlesConfig;

  @Override
  public void onEnable() {
    UserService service = Users.getService();
    service.registerComponent(UserTitles.class);
    addNameElements(service.getNameFactory());

    loadTitles();

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new TitlesCommand());

    ArcadiusServer server = ArcadiusServer.server();
    TitleSettings.add(server.getGlobalSettingsBook());

    TitlePlaceholders.registerAll();

    reloadConfig();
  }

  @Override
  public void onDisable() {
    var nameFactory = Users.getService().getNameFactory();
    nameFactory.removePrefix("title_prefix");
    nameFactory.removeField("title");
    TitlePlaceholders.unregister();
  }

  @Override
  public void reloadConfig() {
    titlesConfig = TomlConfigs.loadPluginConfig(this, TitlesConfig.class);
  }

  void addNameElements(UserNameFactory factory) {
    factory.addProfileField("title", 33, new TitleProfileElement());

    factory.addPrefix("title_prefix", 1, (user, context) -> {
      // Don't display rank prefix if the user has disabled it,
      // only in certain circumstances though
      if (!UserRanks.showRank(context)) {
        return null;
      }

      UserTitles titles = user.getComponent(UserTitles.class);
      UserRank rank = titles.getTitle();

      if (rank == UserRanks.DEFAULT) {
        return null;
      }

      return rank.getPrefix();
    });
  }

  public void loadTitles() {
    UserRanks.clearNonConstants();
    PluginJar.saveResources("ranks.toml", ranksFile());

    SerializationHelper.readAsJson(ranksFile(), wrapper -> {
      for (var e : wrapper.entrySet()) {
        if (!Registries.isValidKey(e.getKey())) {
          LOGGER.warn("{} is an invalid registry key", e.getKey());
          continue;
        }

        if (!(e.getValue().isJsonObject())) {
          LOGGER.warn("Expected {} to be JSON object, was {}",
              e.getKey(), e.getValue()
          );
          continue;
        }

        // IDK if it matters, but even though there's an init call in
        // Bootstrap, this is at the moment, the first call to this class
        // during startup, meaning this call loads the class
        UserRanks.deserialize(e.getValue())
            .apply(s -> {
              LOGGER.warn("Couldn't parse rank at {}: {}", e.getKey(), s);
            }, rank -> {
              UserRanks.REGISTRY.register(e.getKey(), rank);
            });
      }
    });
  }

  private Path ranksFile() {
    return getDataFolder().toPath().resolve("ranks.toml");
  }
}