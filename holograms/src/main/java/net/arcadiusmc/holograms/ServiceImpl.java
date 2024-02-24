package net.arcadiusmc.holograms;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
public class ServiceImpl implements LeaderboardService {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Path path;

  private final Map<String, BoardImpl> boards = new Object2ObjectOpenHashMap<>();

  private final BoardRenderTriggers triggers;

  public ServiceImpl(HologramPlugin plugin) {
    this.path = plugin.getDataFolder().toPath().resolve("leaderboards.json");
    this.triggers = new BoardRenderTriggers(plugin);
  }

  public void createDefaultSources() {
    UserService service = Users.getService();
    var registry = getSources();

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
    SerializationHelper.writeJsonFile(path, this::saveTo);
  }

  public void load() {
    SerializationHelper.readAsJson(path, this::loadFrom);
  }

  private void saveTo(JsonWrapper json) {
    boards.forEach((key, board) -> {
      LeaderboardCodecs.BOARD_CODEC.encode(JsonOps.INSTANCE, board)
          .mapError(s -> "Failed to save leaderboard '" + key + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(element -> json.add(key, element));
    });
  }

  private void loadFrom(JsonObject jsonWrapper) {
    clear();

    for (Entry<String, JsonElement> entry : jsonWrapper.entrySet()) {
      String key = entry.getKey();
      JsonElement element = entry.getValue();

      if (!element.isJsonObject()) {
        LOGGER.error("Can't load leaderboard '{}': Not a JSON object", key);
        continue;
      }

      BoardImpl board = new BoardImpl(key);

      LeaderboardCodecs.BOARD_CODEC.decode(JsonOps.INSTANCE, element, board)
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

  // Returns non-API version of leaderboard
  public Optional<BoardImpl> getBoard(String name) {
    return Optional.ofNullable(boards.get(name));
  }

  @Override
  public Result<Leaderboard> createLeaderboard(String name) {
    if (Strings.isNullOrEmpty(name)) {
      return Result.error("Null/blank name");
    }
    if (!Registries.isValidKey(name)) {
      return Result.error("Invalid key, must match pattern %s", Registries.VALID_KEY_REGEX);
    }
    if (boards.containsKey(name)) {
      return Result.error("Name already in use");
    }

    BoardImpl board = new BoardImpl(name);
    addLeaderboard(board);

    return Result.success(board);
  }

  public void addLeaderboard(BoardImpl board) {
    Objects.requireNonNull(board.getName(), "Boards name is null when adding");
    boards.put(board.getName(), board);
    board.service = this;

    triggers.onAdded(board);
    board.update();
  }

  @Override
  public boolean removeLeaderboard(String name) {
    var removed = boards.remove(name);
    if (removed == null) {
      return false;
    }

    removed.kill();
    removed.service = null;

    triggers.onRemoved(removed);

    return true;
  }

  public void clear() {
    boards.forEach((s, board) -> {
      board.kill();
      board.service = null;
    });
    boards.clear();
    triggers.clear();
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
}
