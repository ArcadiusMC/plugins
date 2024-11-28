package net.arcadiusmc.markets.gui;

import static net.kyori.adventure.text.Component.empty;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.menu.MenuCodecs;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class ShopLists {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Registry<ShopListMenu> registry = Registries.newRegistry();

  @Getter
  private MenuSettings currentSettings;

  public static final ContextSet SET = ContextSet.create();
  public static final ContextOption<Integer> PAGE = SET.newOption(0);

  private final Path file;

  public ShopLists(MarketsPlugin plugin) {
    this.file = plugin.getDataFolder().toPath().resolve("shop-menus.yml");
  }

  public void load() {
    registry.clear();
    PluginJar.saveResources("shop-menus.yml", file);

    SerializationHelper.readAsJson(file, object -> {
      MenuSettings settings;

      if (object.has("settings")) {
        JsonElement elem = object.remove("settings");
        settings = MenuSettings.CODEC.parse(JsonOps.INSTANCE, elem)
            .mapError(s -> "Failed to load menu settings: " + s)
            .resultOrPartial(LOGGER::error)
            .orElse(MenuSettings.DEFAULT);
      } else {
        settings = MenuSettings.DEFAULT;
      }

      this.currentSettings = settings;

      MenuConfig.MAP_CODEC.parse(JsonOps.INSTANCE, object)
          .mapError(s -> "Failed to load market-menus: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(this::loadFrom);
    });
  }

  private void loadFrom(Map<String, MenuConfig> map) {
    for (Entry<String, MenuConfig> entry : map.entrySet()) {
      ShopListMenu menu = new ShopListMenu(PAGE, currentSettings, entry.getValue());
      registry.register(entry.getKey(), menu);
    }
  }

  public record MenuConfig(
      Component title,
      Component headerName,
      List<Component> description,
      List<String> groups,
      int size
  ) {
    static final Codec<MenuConfig> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.COMPONENT
                  .fieldOf("title")
                  .forGetter(MenuConfig::title),

              ExtraCodecs.COMPONENT
                  .fieldOf("header-title")
                  .forGetter(MenuConfig::headerName),

              ExtraCodecs.COMPONENT.listOf()
                  .fieldOf("description")
                  .forGetter(MenuConfig::description),

              Codec.STRING.listOf()
                  .fieldOf("market-groups")
                  .forGetter(MenuConfig::groups),

              MenuCodecs.INVENTORY_SIZE
                  .fieldOf("size")
                  .forGetter(MenuConfig::size)
          )
          .apply(instance, MenuConfig::new);
    });

    static final Codec<Map<String, MenuConfig>> MAP_CODEC
        = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, CODEC);
  }

  public record MenuSettings(
      boolean alignItemNames,
      boolean usePlayerHeadWhenOwned,
      Component availableName,
      Component takenName,
      Component firstLine,
      Component priceLine
  ) {
    static final MenuSettings DEFAULT
        = new MenuSettings(true, true, empty(), empty(), empty(), empty());

    static final Codec<MenuSettings> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.BOOL.fieldOf("align-header-names")
                  .forGetter(MenuSettings::alignItemNames),

              Codec.BOOL.optionalFieldOf("use-owner-head", DEFAULT.usePlayerHeadWhenOwned)
                  .forGetter(MenuSettings::usePlayerHeadWhenOwned),

              ExtraCodecs.COMPONENT.fieldOf("available-name")
                  .forGetter(MenuSettings::availableName),

              ExtraCodecs.COMPONENT.fieldOf("taken-name")
                  .forGetter(MenuSettings::takenName),

              ExtraCodecs.COMPONENT.fieldOf("first-line")
                  .forGetter(MenuSettings::firstLine),

              ExtraCodecs.COMPONENT.fieldOf("price-line")
                  .forGetter(MenuSettings::priceLine)
          )
          .apply(instance, MenuSettings::new);
    });
  }
}
