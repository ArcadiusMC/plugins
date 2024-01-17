package net.arcadiusmc.holograms;

import java.util.Optional;
import java.util.Set;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.Result;

public interface LeaderboardService {

  Registry<LeaderboardSource> getSources();

  Optional<Leaderboard> getLeaderboard(String name);

  Result<Leaderboard> createLeaderboard(String name);

  boolean removeLeaderboard(String name);

  Set<String> getExistingLeaderboards();

  void updateWithSource(Holder<LeaderboardSource> source);
}
