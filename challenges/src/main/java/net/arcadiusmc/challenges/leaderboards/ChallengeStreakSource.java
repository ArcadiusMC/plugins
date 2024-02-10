package net.arcadiusmc.challenges.leaderboards;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.challenges.ChallengeEntry;
import net.arcadiusmc.challenges.ChallengeManager;
import net.arcadiusmc.challenges.Streak;
import net.arcadiusmc.challenges.StreakCategory;
import net.arcadiusmc.leaderboards.LeaderboardScore;
import net.arcadiusmc.leaderboards.LeaderboardSource;
import net.arcadiusmc.leaderboards.PlayerScore;

@RequiredArgsConstructor
public class ChallengeStreakSource implements LeaderboardSource {

  private final StreakCategory category;
  private final ChallengeManager manager;

  private final boolean highest;

  @Override
  public List<LeaderboardScore> getScores() {
    var entries = manager.getEntries();
    List<LeaderboardScore> scores = new ArrayList<>(entries.size());

    for (ChallengeEntry entry : entries) {
      Streak streak = entry.getStreak(category);
      int value = highest ? streak.getHighest() : streak.get();

      if (value == -1) {
        continue;
      }

      scores.add(new PlayerScore(entry.getId(), value));
    }

    return scores;
  }

  @Override
  public OptionalInt getScore(UUID playerId) {
    ChallengeEntry entry = manager.getEntry(playerId);
    Streak streak = entry.getStreak(category);
    return OptionalInt.of(highest ? streak.getHighest() : streak.get());
  }
}
