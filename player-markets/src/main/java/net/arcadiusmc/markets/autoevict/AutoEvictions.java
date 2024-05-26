package net.arcadiusmc.markets.autoevict;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.WorldEditHook;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.autoevict.MarketScanResult.SignShopBlock;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.signshops.ShopManager;
import net.arcadiusmc.signshops.SignShop;
import net.arcadiusmc.signshops.SignShops;
import net.arcadiusmc.signshops.SignShopsPlugin;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.Results;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import net.arcadiusmc.utils.math.AreaSelection;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.TagTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

public class AutoEvictions {

  public static final String AUTO_EVICT_PREFIX = "auto_evict";

  private static final Logger LOGGER = Loggers.getLogger();

  private static final Codec<List<MarketScanResult>> SCAN_FILE_CODEC
      = MarketScanResult.CODEC.listOf().optionalFieldOf("scans", List.of()).codec();

  private final Registry<CriterionType<?>> types = Registries.newFreezable();
  private final List<EvictionCriterion<?>> criteria = new ArrayList<>();

  private final Map<String, List<MarketScanResult>> scans = new HashMap<>();

  private final MarketsPlugin plugin;

  private final Path directory;
  private final Path dataFile;
  private final Path configFile;

  private final Codec<Config> configCodec;

  @Getter
  private Config config = Config.EMPTY;

  private BukkitTask task;
  private Instant lastExecution;

  public AutoEvictions(MarketsPlugin plugin) {
    this.plugin = plugin;

    Path pluginDir  = plugin.getDataFolder().toPath();
    this.directory  = pluginDir.resolve("eviction-system");
    this.configFile = pluginDir.resolve("auto-evictions.yml");
    this.dataFile   = directory.resolve("data.dat");

    // Register types
    types.register(ShopCountCriterion.KEY,        new ShopCountCriterion());
    types.register(ShopStockCriterion.KEY,        new ShopStockCriterion());
    types.register(OwnerInactivityCriterion.KEY,  new OwnerInactivityCriterion());
    types.register(ShopUnusedCriterion.KEY,       new ShopUnusedCriterion());
    types.freeze();

    this.configCodec = Config.createCodec(criterionCodec());
  }

  public void stopTask() {
    task = Tasks.cancel(task);
  }

  public void schedule() {
    stopTask();

    Instant now = Instant.now();

    if (lastExecution == null) {
      lastExecution = now;
    }

    Duration interval = config.validationInterval;
    Instant nextExec = lastExecution.plus(interval);

    if (nextExec.isBefore(now)) {
      validateAll();
      return;
    }

    Duration until = Duration.between(now, nextExec);
    task = Tasks.runLater(this::validateAll, until);
  }

  public void validateAll() {
    MarketsManager manager = plugin.getManager();
    Object2FloatMap<String> aggressionMap = calculateAggression(manager);

    Collection<Market> markets = manager.getMarkets();

    for (Market market : markets) {
      if (market.getOwnerId() == null) {
        continue;
      }

      scanMarket(market, manager, aggressionMap);
    }

    lastExecution = Instant.now();

    save();
    schedule();
  }

  void scanMarket(Market market, MarketsManager manager, Object2FloatMap<String> aggressionMap) {
    if (manager.isMarkedForEviction(market)) {
      return;
    }

    Optional<List<MarketScanResult>> listDataResult = loadScans(market)
        .mapError(s -> "Failed to load scan data list for " + market.getRegionName() + ": " + s)
        .resultOrPartial(LOGGER::error);

    List<MarketScanResult> scans = listDataResult.orElse(new ArrayList<>());
    float aggression = aggressionMap.getFloat(market.getGroupName());

    Optional<MarketScanResult> scanResult = performScan(market, aggression)
        .mapError(s -> "Failed to scan market " + market.getRegionName() + ": " + s)
        .resultOrPartial(LOGGER::error);

    if (scanResult.isEmpty()) {
      return;
    }

    scans.add(scanResult.get());

    List<EvictionCriterion<?>> criterionList = criteria.stream()
        .filter(c -> c.aggressionThreshold() <= aggression)
        .toList();

    if (criterionList.isEmpty()) {
      return;
    }

    for (EvictionCriterion<?> criterion : criterionList) {
      int errorPersistence = testShop(criterion, scans);

      if (errorPersistence < 1) {
        continue;
      }

      if (errorPersistence < criterion.persistence()) {
        continue;
      }

      evict(criterion, market, manager);
      return;
    }
  }

  <V> void evict(EvictionCriterion<V> criterion, Market market, MarketsManager manager) {
    CriterionType<V> type = criterion.type().getValue();
    V value = criterion.value();

    Component reasonText = type.getReasonDisplay(value);

    String source = AUTO_EVICT_PREFIX
        + ";" + criterion.type().getKey()
        + ";" + type.toString(value);

    manager.beginEviction(market, source, reasonText, config.evictionDelay);
  }

  <V> int testShop(EvictionCriterion<V> criterion, List<MarketScanResult> scans) {
    CriterionType<V> type = criterion.type().getValue();

    List<MarketScanResult> reversed = Lists.reverse(scans);
    int persisted = 0;

    for (MarketScanResult marketScanResult : reversed) {
      boolean passed = type.test(criterion.value(), marketScanResult);

      if (passed) {
        break;
      }

      persisted++;
    }

    return persisted;
  }

  Object2FloatMap<String> calculateAggression(MarketsManager manager) {
    Object2FloatMap<String> map = new Object2FloatOpenHashMap<>();
    Collection<Market> markets = manager.getMarkets();

    int globalTotal = markets.size();
    int globalOwned = (int) markets.stream().filter(market -> market.getOwnerId() != null).count();
    map.defaultReturnValue(aggression(globalOwned, globalTotal));

    if (!config.localizedAggression) {
      return map;
    }

    Map<String, List<Market>> byGroup = new HashMap<>();

    for (Market market : markets) {
      String groupName = market.getGroupName();

      if (Strings.isNullOrEmpty(groupName)) {
        continue;
      }

      List<Market> group = byGroup.computeIfAbsent(groupName, s -> new ArrayList<>());
      group.add(market);
    }

    byGroup.forEach((s, markets1) -> {
      float aggression = aggression(markets1);
      map.put(s, aggression);
    });

    return map;
  }

  float aggression(Collection<Market> markets) {
    int total = markets.size();
    int owned = (int) markets.stream().filter(market -> market.getOwnerId() != null).count();
    return aggression(owned, total);
  }

  float aggression(float owned, float total) {
    float value = config.baseAggression + ((owned / total) * config.aggressionMultiplier);

    if (value < 0) {
      return 0f;
    }
    if (value > 1) {
      return 1f;
    }

    return value;
  }

  public DataResult<MarketScanResult> performScan(Market market, float aggression) {
    World world = market.getWorld();

    if (world == null) {
      return Results.error("Market '%s' has null world", market.getRegionName());
    }

    Optional<ProtectedRegion> regionOpt = market.getRegion();

    if (regionOpt.isEmpty()) {
      return Results.error("Market '%s' is missing its world-guard region", market.getRegionName());
    }

    UUID ownerId = market.getOwnerId();
    if (ownerId == null) {
      return Results.error("Market '%s' has no owner", market.getRegionName());
    }

    ProtectedRegion region = regionOpt.get();
    AreaSelection selection = WorldEditHook.wrap(world, region);

    ShopManager shops = SignShopsPlugin.plugin().getManager();

    List<SignShopBlock> blocks = new ObjectArrayList<>();

    for (Block block : selection) {
      if (!SignShops.isShop(block)) {
        continue;
      }

      SignShop shop = shops.getShop(block);
      ItemStack exampleItem = shop.getExampleItem();

      // Don't count broken shops as shops at all
      if (ItemStacks.isEmpty(exampleItem)) {
        continue;
      }

      int maxStackSize = exampleItem.getType().getMaxStackSize();
      int inventorySize = shop.getInventory().getSize();
      int stock = countItems(shop.getInventory());
      long lastUse = shop.getLastInteraction();

      SignShopBlock signShopBlock = new SignShopBlock(
          maxStackSize,
          stock,
          exampleItem.getAmount(),
          inventorySize,
          lastUse,
          shop.getType()
      );

      blocks.add(signShopBlock);
    }

    User owner = Users.get(ownerId);
    long lastOnline = owner.getTime(TimeField.LAST_LOGIN);

    return Results.success(
        new MarketScanResult(blocks, lastOnline, aggression, System.currentTimeMillis())
    );
  }

  private int countItems(Inventory inventory) {
    int result = 0;
    var it = ItemStacks.nonEmptyIterator(inventory);

    while (it.hasNext()) {
      ItemStack n = it.next();
      result += n.getAmount();
    }

    return result;
  }

  public void load() {
    criteria.clear();

    PluginJar.saveResources("auto-evictions.yml", configFile);
    SerializationHelper.readAsJson(configFile, this::loadFrom);

    loadDatafile();
  }

  private void loadFrom(JsonObject object) {
    configCodec.parse(JsonOps.INSTANCE, object)
        .mapError(s -> "Failed to load auto-eviction config: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(config -> this.config = config);
  }

  private Path getFile(String marketName) {
    return directory.resolve("scans_" + marketName + ".dat");
  }

  private DataResult<List<MarketScanResult>> loadScans(Market market) {
    String key = market.getRegionName();

    if (scans.containsKey(key)) {
      return Results.success(scans.get(key));
    }

    Path file = getFile(key);

    if (!Files.exists(file)) {
      List<MarketScanResult> results = new ArrayList<>();
      scans.put(key, results);
      return Results.success(results);
    }

    return SerializationHelper.readTag(file)
        .flatMap(tag -> SCAN_FILE_CODEC.parse(TagOps.OPS, tag))
        .map(ArrayList::new)
        .map(list -> {
          scans.put(key, list);
          return list;
        });
  }

  public void save() {
    scans.forEach((s, marketScanResults) -> {
      Path file = getFile(s);

      Optional<CompoundTag> opt = SCAN_FILE_CODEC.encodeStart(TagOps.OPS, marketScanResults)
          .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
          .mapError(s1 -> "Failed to save scans file for " + s + ": " + s1)
          .resultOrPartial(LOGGER::error);

      if (opt.isEmpty()) {
        return;
      }

      SerializationHelper.writeTag(file, opt.get());
    });

    saveDataFile();
  }

  private void saveDataFile() {
    SerializationHelper.writeTagFile(dataFile, tag -> {
      if (lastExecution != null) {
        tag.putLong("last_scan", lastExecution.toEpochMilli());
      }

    });
  }

  private void loadDatafile() {
    lastExecution = null;

    SerializationHelper.readTagFile(dataFile, tag -> {
      if (tag.contains("last_scan", TagTypes.longType())) {
        long l = tag.getLong("last_scan");
        lastExecution = Instant.ofEpochMilli(l);
      }
    });
  }

  Codec<List<EvictionCriterion<?>>> criterionCodec() {
    return new Codec<>() {

      @Override
      public <T> DataResult<T> encode(
          List<EvictionCriterion<?>> input,
          DynamicOps<T> ops,
          T prefix
      ) {
        return Results.error("NO-OP");
      }

      @Override
      public <T> DataResult<Pair<List<EvictionCriterion<?>>, T>> decode(DynamicOps<T> ops,
          T input) {
        return ops.getMap(input)
            .flatMap(tMapLike -> {
              DataResult<List<EvictionCriterion<?>>> result = Results.success(new ArrayList<>());
              boolean anyFound = false;

              for (Holder<CriterionType<?>> entry : types.entries()) {
                T value = tMapLike.get(entry.getKey());

                if (value == null) {
                  continue;
                }

                anyFound = true;

                @SuppressWarnings("rawtypes")
                Holder holder = entry;

                @SuppressWarnings("unchecked")
                Codec<EvictionCriterion<?>> codec = EvictionCriterion.codec(holder);

                result = result.apply2(
                    (criterionList, pair) -> {
                      criterionList.add(pair.getFirst());
                      return criterionList;
                    },
                    codec.decode(ops, value)
                );
              }

              if (!anyFound) {
                String joinedKeys = "'" + Joiner.on("', '").join(types.keys()) + "'";
                return Results.error("Missing any of the following keys: %s", joinedKeys);
              }

              return result.map(criterionList -> Pair.of(criterionList, input));
            });
      }
    };
  }


  public record Config(
      boolean enabled,
      Duration validationInterval,
      Duration evictionDelay,
      float baseAggression,
      float aggressionMultiplier,
      boolean localizedAggression,
      List<EvictionCriterion<?>> criteria
  ) {
    static final Config EMPTY
        = new Config(false, Duration.ofDays(7), Duration.ofDays(14), 0.1f, 1f, true, List.of());

    static Codec<Config> createCodec(Codec<List<EvictionCriterion<?>>> criterionCodec) {
      return RecordCodecBuilder.create(instance -> {
        return instance
            .group(
                Codec.BOOL.optionalFieldOf("enabled", EMPTY.enabled)
                    .forGetter(Config::enabled),

                ExtraCodecs.DURATION.optionalFieldOf("validation-interval", EMPTY.validationInterval)
                    .forGetter(Config::validationInterval),

                ExtraCodecs.DURATION.optionalFieldOf("eviction-delay", EMPTY.evictionDelay)
                    .forGetter(Config::evictionDelay),

                Codec.FLOAT.optionalFieldOf("base-aggression", EMPTY.baseAggression)
                    .forGetter(Config::baseAggression),

                Codec.FLOAT.optionalFieldOf("aggression-multiplier", EMPTY.aggressionMultiplier)
                    .forGetter(Config::aggressionMultiplier),

                Codec.BOOL.optionalFieldOf("localized-aggression", true)
                        .forGetter(Config::localizedAggression),

                criterionCodec.optionalFieldOf("criteria", List.of())
                    .forGetter(Config::criteria)
            )
            .apply(instance, Config::new);
      });
    }
  }
}
