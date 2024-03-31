package net.arcadiusmc.markets;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import java.nio.file.Path;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.math.WorldVec3i;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.slf4j.Logger;

public class MarketResets {

  private static final Logger LOGGER = Loggers.getLogger();

  private Config config = Config.EMPTY;

  private final Path configFile;

  public MarketResets(MarketsPlugin plugin) {
    Path pluginDir = plugin.getDataFolder().toPath();
    this.configFile = pluginDir.resolve("resets.yml");
  }

  public void load() {
    PluginJar.saveResources("resets.yml", configFile);

    SerializationHelper.readAsJson(configFile, jsonObject -> {
      Config.CODEC.parse(JsonOps.INSTANCE, jsonObject)
          .mapError(s -> "Failed to read '" + configFile + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(config -> this.config = config);
    });
  }

  public void reset(Market market) {
    copyPasteMarket(market, true);
  }

  public void onClaim(Market market) {
    if (!config.copyOnClaim) {
      return;
    }

    copyPasteMarket(market, false);
  }

  private void copyPasteMarket(Market market, boolean copyFromTemplate) {
    if (config.offset.isEmpty() || !config.enabled) {
      return;
    }

    World world = market.getWorld();
    Optional<ProtectedRegion> regionOpt = market.getRegion();

    if (regionOpt.isEmpty()) {
      LOGGER.error("Cannot reset market '{}': WorldGuard regionOpt not found", market.getRegionName());
      return;
    }

    if (world == null) {
      return;
    }

    ProtectedRegion region = regionOpt.get();
    WorldVec3i offset = config.offset.toWorldVec(world);

    Region shopWeRegion = WorldEditRegionConverter.convertToRegion(region);
    shopWeRegion.setWorld(BukkitAdapter.adapt(world));

    Region resetWeRegion = shopWeRegion.clone();
    resetWeRegion.shift(BlockVector3.at(offset.x(), offset.y(), offset.z()));
    resetWeRegion.setWorld(BukkitAdapter.adapt(offset.getWorld()));

    if (copyFromTemplate) {
      copyRegion(resetWeRegion, shopWeRegion);
    } else {
      copyRegion(shopWeRegion, resetWeRegion);
    }
  }

  private void copyRegion(Region copyRegion, Region destination) {
    ForwardExtentCopy copy = new ForwardExtentCopy(
        copyRegion.getWorld(),
        copyRegion,
        copyRegion.getMinimumPoint(),
        destination.getWorld(),
        destination.getMinimumPoint()
    );

    Operations.complete(copy);
  }

  private record Config(Offset offset, boolean copyOnClaim, boolean enabled) {
    static final Config EMPTY = new Config(Offset.EMPTY, false, false);

    static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Offset.CODEC.optionalFieldOf("template-offset", Offset.EMPTY)
                  .forGetter(o -> o.offset),

              Codec.BOOL.optionalFieldOf("copy-on-claim", false)
                  .forGetter(o -> o.copyOnClaim),

              Codec.BOOL.optionalFieldOf("enabled", true)
                  .forGetter(o -> o.enabled)
          )
          .apply(instance, Config::new);
    });
  }

  private record Offset(String world, int x, int y, int z) {
    static final Offset EMPTY = new Offset("", 0, 0, 0);

    static final Codec<Offset> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.STRING.optionalFieldOf("world", "").forGetter(o -> o.world),
              Codec.INT.optionalFieldOf("x", 0).forGetter(o -> o.x),
              Codec.INT.optionalFieldOf("y", 0).forGetter(o -> o.y),
              Codec.INT.optionalFieldOf("z", 0).forGetter(o -> o.z)
          )
          .apply(instance, Offset::new);
    });

    public boolean isEmpty() {
      return x == 0 && y == 0 && z == 0;
    }

    public WorldVec3i toWorldVec(World marketWorld) {
      World world = Strings.isNullOrEmpty(this.world) ? null : Bukkit.getWorld(this.world);

      if (world == null) {
        return new WorldVec3i(marketWorld, x, y, z);
      }

      return new WorldVec3i(world, x, y, z);
    }
  }
}
