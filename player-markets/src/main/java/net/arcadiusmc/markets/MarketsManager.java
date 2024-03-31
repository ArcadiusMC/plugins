package net.arcadiusmc.markets;

import com.mojang.serialization.Codec;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.mail.Mail;
import net.arcadiusmc.mail.MailSendFlag;
import net.arcadiusmc.mail.MailService;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

public class MarketsManager {

  private static final Logger LOGGER = Loggers.getLogger();

  private static final Codec<List<Eviction>> EVICTION_CODEC = Eviction.CODEC.listOf()
      .fieldOf("data")
      .codec();

  private final Map<String, Market> byName = new Object2ObjectOpenHashMap<>();
  private final Map<UUID, Market> byOwner = new Object2ObjectOpenHashMap<>();
  private final Map<String, Eviction> evictions = new Object2ObjectOpenHashMap<>();

  @Getter
  private final MarketsPlugin plugin;

  private final Path marketsDirectory;
  private final Path evictionsFile;

  @Getter
  private boolean serverLoaded;

  private BukkitTask marketTickTask;

  public MarketsManager(MarketsPlugin plugin) {
    this.plugin = plugin;

    Path pluginDir = plugin.getDataFolder().toPath();
    this.marketsDirectory = pluginDir.resolve("markets");
    this.evictionsFile = pluginDir.resolve("evictions.dat");
  }

  public void onServerLoaded() {
    serverLoaded = true;

    byName.forEach((string, market) -> {
      market.onAdded(this);
    });

    Instant now = Instant.now();
    evictions.forEach((string, eviction) -> {
      eviction.task = Tasks.runLater(eviction, eviction.getDelay(now));
    });

    scheduleMarketTicker();
  }

  void scheduleMarketTicker() {
    marketTickTask = Tasks.cancel(marketTickTask);

    MarketsConfig config = plugin.getPluginConfig();

    marketTickTask = Tasks.runTimer(
        this::tickMarkets,
        config.marketTickInterval(),
        config.marketTickInterval()
    );
  }

  private void tickMarkets() {
    if (Bukkit.hasWhitelist()) {
      return;
    }

    Instant now = Instant.now();

    for (Market value : byName.values()) {
      value.tick(now);
    }
  }

  public boolean isMarkedForEviction(Market market) {
    return evictions.containsKey(market.getRegionName());
  }

  public boolean beginEviction(Market market, String source, Component reason, Duration delay) {
    Objects.requireNonNull(market, "Null market");
    Objects.requireNonNull(source, "Null source");
    Objects.requireNonNull(reason, "Null reason");
    Objects.requireNonNull(delay, "Null delay");

    if (isMarkedForEviction(market)) {
      return false;
    }

    Instant now = Instant.now();
    Instant end = now.plus(delay);

    Eviction eviction = new Eviction(market.getRegionName(), source, reason, now, end);
    addEviction(eviction);

    Mail mail = Mail.builder()
        .message(
            Messages.render("markets.evictionStart")
                .addValue("date", end)
                .addValue("reason", reason)
                .asComponent()
        )
        .target(market.getOwnerId())
        .build();

    MailService.service().send(mail, MailSendFlag.NO_DISCORD);
    return true;
  }

  public Eviction getEviction(Market market) {
    return evictions.get(market.getRegionName());
  }

  private void addEviction(Eviction eviction) {
    eviction.manager = this;

    if (serverLoaded) {
      eviction.task = Tasks.runLater(eviction, eviction.getDelay(Instant.now()));
    }

    evictions.put(eviction.getMarketName(), eviction);
  }

  public boolean stopEviction(Market market) {
    Eviction eviction = evictions.remove(market.getRegionName());

    if (eviction == null) {
      return false;
    }

    eviction.task = Tasks.cancel(eviction.task);
    eviction.manager = null;

    return true;
  }

  void removeEviction(Eviction eviction) {
    evictions.remove(eviction.getMarketName());
  }

  void onUnclaim(Market market) {
    byOwner.remove(market.getOwnerId());
  }

  void onClaim(Market market) {
    byOwner.put(market.getOwnerId(), market);
  }

  public Market getByOwner(UUID ownerId) {
    Objects.requireNonNull(ownerId, "Null ownerId");
    return byOwner.get(ownerId);
  }

  public Market getMarket(String name) {
    Objects.requireNonNull(name, "Null name");
    return byName.get(name);
  }

  public void addMarket(Market market) {
    Objects.requireNonNull(market, "Null market");
    byName.put(market.getRegionName(), market);

    if (market.getOwnerId() != null) {
      byOwner.put(market.getOwnerId(), market);
    }

    if (serverLoaded) {
      market.onAdded(this);
    }
  }

  public void removeMarket(Market market) {
    Objects.requireNonNull(market, "Null market");
    market = byName.remove(market.getRegionName());

    if (market.getOwnerId() != null) {
      byOwner.remove(market.getOwnerId());
    }

    if (serverLoaded) {
      market.onRemoved(this);
    }
  }

  public void deleteMarket(Market market) {
    if (market.getOwnerId() != null) {
      market.unclaim();
    }
    if (market.getMerged() != null) {
      market.unmerge();
    }

    World world = market.getWorld();
    if (world != null) {
      for (Entrance entrance : market.getEntrances()) {
        entrance.remove(world);
      }
    }

    market.clearConnected();
    removeMarket(market);

    Path file = getMarketFile(market);
    PathUtil.safeDelete(file);
  }

  public Collection<Market> getMarkets() {
    return Collections.unmodifiableCollection(byName.values());
  }

  public Collection<Market> getOverlapping(World world, Vector3i position) {
    return byName.values()
        .stream()
        .filter(market -> Objects.equals(world, market.getWorld()))
        .filter(market -> {
          Optional<ProtectedRegion> regionOpt = market.getRegion();

          if (regionOpt.isEmpty()) {
            return false;
          }

          ProtectedRegion region = regionOpt.get();
          return region.contains(position.x(), position.y(), position.z());
        })
        .collect(Collectors.toUnmodifiableSet());
  }

  public void load() {
    loadMarkets();
    loadEvictions();
  }

  public void save() {
    saveEvictions();
    saveMarkets();
  }

  private void loadMarkets() {
    if (serverLoaded) {
      byName.forEach((s, market) -> {
        market.onRemoved(this);
      });
    }

    byOwner.clear();
    byName.clear();

    PathUtil.iterateDirectory(marketsDirectory, false, true, path -> {
      if (!path.toString().endsWith(".dat")) {
        return;
      }

      SerializationHelper.readTagFile(path, tag -> {
        Market.CODEC.parse(TagOps.OPS, tag)
            .mapError(s -> "Failed to load market from file " + path + ": " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(this::addMarket);
      });
    });
  }

  private void saveMarkets() {
    byName.forEach((s, market) -> {
      Path path = getMarketFile(market);

      Market.CODEC.encodeStart(TagOps.OPS, market)
          .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
          .mapError(s1 -> "Failed to save market " + market.getRegionName() + ": " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(tag -> {
            SerializationHelper.writeTag(path, tag);
          });
    });
  }

  private Path getMarketFile(Market market) {
    String name = market.getRegionName();
    String worldName = market.getWorldName();
    return marketsDirectory.resolve(worldName + "." + name + ".dat");
  }

  private void loadEvictions() {
    for (Eviction value : evictions.values()) {
      value.cancel();
    }

    evictions.clear();

    SerializationHelper.readTagFile(evictionsFile, tag -> {
      if (tag.isEmpty()) {
        return;
      }

      EVICTION_CODEC.parse(TagOps.OPS, tag)
          .mapError(string -> "Failed to read evictions from " + evictionsFile + ": " + string)
          .resultOrPartial(LOGGER::error)
          .ifPresent(evictions -> {
            for (Eviction eviction : evictions) {
              if (this.evictions.containsKey(eviction.getMarketName())) {
                LOGGER.warn("Duplicate eviction for market '{}'", eviction.getMarketName());
                continue;
              }

              if (!byName.containsKey(eviction.getMarketName())) {
                LOGGER.warn("Loaded eviction targetting unknown shop '{}'",
                    eviction.getMarketName()
                );
              }

              addEviction(eviction);
            }
          });
    });
  }

  private void saveEvictions() {
    EVICTION_CODEC.encodeStart(TagOps.OPS, new ArrayList<>(evictions.values()))
        .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
        .mapError(string -> "Failed to save evictions: " + string)
        .resultOrPartial(LOGGER::error)
        .ifPresent(tag -> {
          SerializationHelper.writeTag(evictionsFile, tag);
        });
  }
}
