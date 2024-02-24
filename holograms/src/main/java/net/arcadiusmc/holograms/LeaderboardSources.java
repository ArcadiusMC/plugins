package net.arcadiusmc.holograms;

import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.io.Results;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;

public class LeaderboardSources {

  public static final Registry<LeaderboardSource> SOURCES = Registries.newRegistry();

  public static final String OBJECTIVE_PREFIX = "objective/";

  public static final LeaderboardSource DUMMY = DummySource.INSTANCE;

  public static final Holder<LeaderboardSource> DUMMY_HOLDER = SOURCES.register("dummy", DUMMY);

  public static DataResult<Holder<LeaderboardSource>> get(String name) {
    return SOURCES.getHolder(name)
        .map(Results::success)
        .or(() -> {
          if (!name.startsWith(OBJECTIVE_PREFIX)) {
            return Optional.empty();
          }

          String objName = name.substring(OBJECTIVE_PREFIX.length());

          Objective objective = Bukkit.getScoreboardManager()
              .getMainScoreboard()
              .getObjective(objName);

          if (objective == null) {
            return Optional.of(Results.error("Unknown objective '%s'", objName));
          }

          Holder<LeaderboardSource> holder = SOURCES.register(name, new ObjectiveSource(objName));
          return Optional.of(Results.success(holder));
        })
        .orElse(Results.error("Unknown source '%s'", name));
  }
}
