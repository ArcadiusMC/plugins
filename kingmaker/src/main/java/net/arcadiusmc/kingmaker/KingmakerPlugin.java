package net.arcadiusmc.kingmaker;

import com.mojang.serialization.JsonOps;
import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.text.placeholder.PlaceholderService;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class KingmakerPlugin extends JavaPlugin {

  private Config pluginConfig;
  private Kingmaker kingmaker;

  private PeriodicalSaver saver;

  @Override
  public void onEnable() {
    kingmaker = new Kingmaker(this);
    load();

    AnnotatedCommandContext context = Commands.createAnnotationContext();
    context.registerCommand(new KingmakerCommand(this));

    saver = PeriodicalSaver.create(this::save, Duration.ofMinutes(30));
    saver.start();

    PlaceholderService service = Placeholders.getService();
    service.getDefaults().add("emperor", new MonarchPlaceholder(kingmaker));
  }

  @Override
  public void onDisable() {
    save();

    PlaceholderService service = Placeholders.getService();
    service.getDefaults().remove("emperor");

    if (saver != null) {
      saver.stop();
    }
  }

  public void load() {
    reloadConfig();

    if (kingmaker != null) {
      kingmaker.load();
    }
  }

  public void save() {
    if (kingmaker != null) {
      kingmaker.save();
    }
  }

  @Override
  public void reloadConfig() {
    PluginJar.saveResources("config.yml");

    SerializationHelper.readAsJson(
        getDataFolder().toPath().resolve("config.yml"),
        object -> {
          Config.CODEC.parse(JsonOps.INSTANCE, object)
              .mapError(s -> "Failed to load kingmaker config: " + s)
              .resultOrPartial(getSLF4JLogger()::error)
              .ifPresent(config -> pluginConfig = config);
        }
    );
  }
}
