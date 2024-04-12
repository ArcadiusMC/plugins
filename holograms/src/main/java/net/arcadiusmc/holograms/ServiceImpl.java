package net.arcadiusmc.holograms;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.slf4j.Logger;

@Getter
public class ServiceImpl implements HologramService {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Path leaderboardsFile;
  private final Path hologramsFile;

  private final Map<String, BoardImpl> boards = new Object2ObjectOpenHashMap<>();
  private final Map<String, TextImpl> texts = new Object2ObjectOpenHashMap<>();

  private final BoardRenderTriggers triggers;

  public ServiceImpl(HologramPlugin plugin) {
    Path pluginDir = plugin.getDataFolder().toPath();

    this.leaderboardsFile = pluginDir.resolve("leaderboards.json");
    this.hologramsFile = pluginDir.resolve("holograms.json");

    this.triggers = new BoardRenderTriggers(plugin);
  }

  public void createDefaultSources() {
    UserService service = Users.getService();
    Registry<LeaderboardSource> registry = getSources();

    registry.register("rhines", new ScoreMapSource(service.getBalances()));
    registry.register("gems", new ScoreMapSource(service.getGems()));

    registry.register("playtime/total",
        new ScoreMapSource(service.getPlaytime(), 60f * 60f)
    );

    registry.register("playtime/monthly",
        new ScoreMapSource(service.getMonthlyPlaytime(), 60f * 60f)
    );
  }

  public void save() {
    SerializationHelper.writeJsonFile(leaderboardsFile, this::saveLeaderboardsTo);
    SerializationHelper.writeJsonFile(hologramsFile, this::saveHologramsTo);
  }

  public void load() {
    clear();

    SerializationHelper.readAsJson(leaderboardsFile, this::loadLeaderboardsFrom);
    SerializationHelper.readAsJson(hologramsFile, this::loadHologramsFrom);
  }

  private void saveHologramsTo(JsonWrapper json) {
    texts.forEach((key, text) -> {
      HologramCodecs.TEXT_CODEC.encode(JsonOps.INSTANCE, text)
          .mapError(s -> "Failed to save hologram '" + key + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(jsonElement -> json.add(key, jsonElement));
    });
  }

  private void loadHologramsFrom(JsonObject json) {
    for (Entry<String, JsonElement> entry : json.entrySet()) {
      String key = entry.getKey();
      JsonElement element = entry.getValue();

      if (!element.isJsonObject()) {
        LOGGER.error("Can't load hologram '{}': Not a JSON object", key);
        continue;
      }

      TextImpl text = new TextImpl(key);

      HologramCodecs.TEXT_CODEC.decode(JsonOps.INSTANCE, element, text)
          .mapError(s -> "Failed to load hologram '" + text + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(this::addHologram);
    }
  }

  private void saveLeaderboardsTo(JsonWrapper json) {
    boards.forEach((key, board) -> {
      HologramCodecs.BOARD_CODEC.encode(JsonOps.INSTANCE, board)
          .mapError(s -> "Failed to save leaderboard '" + key + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(element -> json.add(key, element));
    });
  }

  private void loadLeaderboardsFrom(JsonObject jsonWrapper) {
    for (Entry<String, JsonElement> entry : jsonWrapper.entrySet()) {
      String key = entry.getKey();
      JsonElement element = entry.getValue();

      if (!element.isJsonObject()) {
        LOGGER.error("Can't load leaderboard '{}': Not a JSON object", key);
        continue;
      }

      BoardImpl board = new BoardImpl(key);

      HologramCodecs.BOARD_CODEC.decode(JsonOps.INSTANCE, element, board)
          .mapError(s -> "Failed to load leaderboard '" + s + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(this::addLeaderboard);
    }
  }

  @Override
  public Registry<LeaderboardSource> getSources() {
    return LeaderboardSources.SOURCES;
  }

  @Override
  public Optional<Leaderboard> getLeaderboard(String name) {
    return Optional.ofNullable(boards.get(name));
  }

  @Override
  public Optional<TextHologram> getHologram(String name) {
    return Optional.ofNullable(texts.get(name));
  }

  // Returns non-API version of leaderboard
  public Optional<BoardImpl> getBoard(String name) {
    return Optional.ofNullable(boards.get(name));
  }

  @Override
  public Result<Leaderboard> createLeaderboard(String name) {
    return create(name, boards, BoardImpl::new).map(board -> board);
  }

  @Override
  public Result<TextHologram> createHologram(String name) {
    return create(name, texts, TextImpl::new).map(text -> text);
  }

  private <T extends Hologram> Result<T> create(
      String name,
      Map<String, T> map,
      Function<String, T> ctor
  ) {
    if (Strings.isNullOrEmpty(name)) {
      return Result.error("Null/blank name");
    }
    if (!Registries.isValidKey(name)) {
      return Result.error("Invalid key, must match pattern %s", Registries.VALID_KEY_REGEX);
    }
    if (map.containsKey(name)) {
      return Result.error("Name already in use");
    }

    T value = ctor.apply(name);
    add(value, map);

    return Result.success(value);
  }

  public void addLeaderboard(BoardImpl board) {
    add(board, boards);
  }

  public void addHologram(TextImpl text) {
    add(text, texts);
  }

  private <T extends Hologram> void add(T value, Map<String, T> map) {
    String name = value.getName();
    Objects.requireNonNull(name, "Boards name is null when adding");

    map.put(name, value);
    value.service = this;

    triggers.onAdded(value);
    value.update();
  }

  @Override
  public boolean removeLeaderboard(String name) {
    return remove(name, boards);
  }

  @Override
  public boolean removeHologram(String name) {
    return remove(name, texts);
  }

  private boolean remove(String name, Map<String, ? extends Hologram> map) {
    Objects.requireNonNull(name, "Null name");

    Hologram removed = map.remove(name);
    if (removed == null) {
      return false;
    }

    removed.kill();
    removed.service = null;

    triggers.onRemoved(removed);

    return true;
  }

  public void clear() {
    clearMap(boards);
    clearMap(texts);
    triggers.clear();
  }

  private void clearMap(Map<String, ? extends Hologram> map) {
    map.forEach((s, text) -> {
      text.kill();
      text.service = null;
    });
    map.clear();
  }

  @Override
  public void updateWithSource(Holder<LeaderboardSource> source) {
    boards.forEach((string, board) -> {
      if (!Objects.equals(source, board.getSource())) {
        return;
      }

      board.update();
    });
  }

  @Override
  public Set<String> getExistingLeaderboards() {
    return Collections.unmodifiableSet(boards.keySet());
  }

  @Override
  public Set<String> getExistingHolograms() {
    return Collections.unmodifiableSet(texts.keySet());
  }

  public Collection<TextImpl> getHolograms() {
    return texts.values();
  }
}
