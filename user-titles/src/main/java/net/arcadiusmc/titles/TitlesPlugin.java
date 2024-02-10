package net.arcadiusmc.titles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Map;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.titles.commands.TitlesCommand;
import net.arcadiusmc.titles.listener.PlayerJoinListener;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.UserNameFactory;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class TitlesPlugin extends JavaPlugin {

  private static final Logger LOGGER = Loggers.getLogger();

  private final MessageList messageList = MessageList.create();

  private Path ranksFile;
  private Path tiersFile;

  @Override
  public void onEnable() {
    Path dataFolder = getDataFolder().toPath();
    ranksFile = dataFolder.resolve("ranks.yml");
    tiersFile = dataFolder.resolve("tiers.yml");

    Messages.MESSAGE_LIST.addChild("titles", messageList);

    UserService service = Users.getService();
    service.registerComponent(UserTitles.class);
    addNameElements(service.getNameFactory());

    load();

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new TitlesCommand());

    Events.register(new PlayerJoinListener());

    ArcadiusServer server = ArcadiusServer.server();
    TitleSettings.add(server.getGlobalSettingsBook());

    TitlePlaceholders.registerAll();

    reloadConfig();
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild("titles");

    var nameFactory = Users.getService().getNameFactory();
    nameFactory.removePrefix("title_prefix");
    nameFactory.removeField("title");
    TitlePlaceholders.unregister();
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
  }

  void addNameElements(UserNameFactory factory) {
    factory.addProfileField("title", 33, new TitleProfileElement());

    factory.addPrefix("title_prefix", 1, (user, context) -> {
      // Don't display rank prefix if the user has disabled it,
      // only in certain circumstances though
      if (!Titles.showRank(context)) {
        return null;
      }

      UserTitles titles = user.getComponent(UserTitles.class);
      Title rank = titles.getTitle();

      if (rank == Titles.DEFAULT) {
        return null;
      }

      return rank.getPrefix();
    });
  }

  public void load() {
    clearNonConstants(Titles.REGISTRY);
    clearNonConstants(Tiers.REGISTRY);

    PluginJar.saveResources("ranks.yml", ranksFile);
    PluginJar.saveResources("tiers.yml", tiersFile);

    loadToRegistry(tiersFile, "tiers", Tiers.REGISTRY, TitleCodecs.TIER_MAP);
    loadToRegistry(ranksFile, "ranks", Titles.REGISTRY, TitleCodecs.RANK_MAP);
  }

  static <R> void loadToRegistry(
      Path path,
      String errorPrefix,
      Registry<R> registry,
      Codec<Map<String, R>> codec
  ) {
    SerializationHelper.readAsJson(path, json -> {
      codec.parse(JsonOps.INSTANCE, json)
          .mapError(s -> "Failed to load " + errorPrefix + ": " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(map -> {
            map.forEach((s, r) -> {
              if (s.equals("example")) {
                return;
              }

              registry.register(s, r);
            });
          });
    });
  }

  static <R extends ReloadableElement> void clearNonConstants(Registry<R> registry) {
    registry.removeIf(rHolder -> rHolder.getValue().isReloadable());
  }
}