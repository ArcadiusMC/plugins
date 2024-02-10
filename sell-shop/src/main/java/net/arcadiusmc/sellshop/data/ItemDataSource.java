package net.arcadiusmc.sellshop.data;

import com.google.gson.JsonElement;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.sellshop.SellShopPlugin;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.Results;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.Material;
import org.slf4j.Logger;

public class ItemDataSource {

  private static final Logger LOGGER = Loggers.getLogger();

  private static final ArgumentType<Material> MATERIAL_PARSER
      = ArgumentTypes.registry(org.bukkit.Registry.MATERIAL, "Material");

  private final Registry<SimpleDataMap> registry;

  @Getter
  private final CombinedDataMap globalPrices;

  private final Path directory;
  private final Path loaderFile;

  public ItemDataSource(SellShopPlugin plugin) {
    this.directory = PathUtil.pluginPath(plugin, "prices");
    this.loaderFile = directory.resolve("loader.yml");

    this.registry = Registries.newRegistry();
    this.globalPrices = new CombinedDataMap();
  }

  public Codec<PricePair> codec() {
    return Codec.STRING
        .comapFlatMap(
            s -> {
              return ExtraCodecs.<DataResult<PricePair>>safeParse(s, reader -> {
                String fileKey = Arguments.RESOURCE_KEY.parse(reader);
                reader.expect(':');
                Material material = MATERIAL_PARSER.parse(reader);

                SimpleDataMap map = registry.orNull(fileKey);

                if (map == null) {
                  return Results.error("Unknown price file '%s'", fileKey);
                }

                ItemSellData data = map.getData(material);

                if (data == null) {
                  return Results.error("Price source '%s' has no data entry for '%s'",
                      fileKey, material.key()
                  );
                }

                return Results.success(new PricePair(fileKey, data, globalPrices.hasSource(map)));
              }).flatMap(dataResult -> dataResult);
            },
            pricePair -> pricePair.fileName() + ":" + pricePair.data().getMaterial().getKey().value()
        );
  }

  public void load() {
    registry.clear();
    globalPrices.clearSources();

    PluginJar.saveResources("prices", directory);

    SerializationHelper.readAsJson(loaderFile, object -> {
      Optional<Map<String, PriceFileEntry>> opt = Codecs.MAP_CODEC.parse(JsonOps.INSTANCE, object)
          .mapError(s -> "Failed to read loader.yml: " + s)
          .resultOrPartial(LOGGER::error);

      if (opt.isEmpty()) {
        return;
      }

      Map<String, PriceFileEntry> map = opt.get();
      loadEntries(map);
    });
  }

  private void loadEntries(Map<String, PriceFileEntry> map) {
    for (Entry<String, PriceFileEntry> entry : map.entrySet()) {
      String id = entry.getKey();
      PriceFileEntry value = entry.getValue();

      if (id.equals("example")) {
        continue;
      }

      Path file = directory.resolve(value.fileName() + ".json");

      if (!Files.exists(file)) {
        LOGGER.error("{}: file {} doesn't exist", id, file);
        continue;
      }

      JsonElement pricesElement;

      try {
        pricesElement = JsonUtils.readFile(file);
      } catch (IOException exc) {
        LOGGER.error("{}: IO exception reading price file {}", id, file);
        continue;
      }

      Optional<SimpleDataMap> opt = Codecs.PRICE_MAP.parse(JsonOps.INSTANCE, pricesElement)
          .mapError(s -> id + ": Failed to load prices from file " + file + ": " + s)
          .resultOrPartial(LOGGER::error);

      if (opt.isEmpty()) {
        continue;
      }

      SimpleDataMap priceMap = opt.get();

      registry.register(id, priceMap);

      if (value.global()) {
        globalPrices.addSource(priceMap);
      }

      LOGGER.debug("Loaded price source '{}' from file {}", id, file);
    }
  }
}
