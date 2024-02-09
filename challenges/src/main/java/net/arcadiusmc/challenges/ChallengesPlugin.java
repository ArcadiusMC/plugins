package net.arcadiusmc.challenges;

import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.challenges.commands.CommandChallenges;
import net.arcadiusmc.challenges.leaderboards.ChallengeStreakSource;
import net.arcadiusmc.challenges.listeners.ChallengeListeners;
import net.arcadiusmc.leaderboards.LeaderboardSource;
import net.arcadiusmc.leaderboards.Leaderboards;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ChallengesPlugin extends JavaPlugin {

  private ChallengeManager challenges;
  private ChallengeConfig pluginConfig;

  private PeriodicalSaver saver;

  @Override
  public void onEnable() {
    DataFix.execute();

    challenges = new ChallengeManager(this);

    saver = PeriodicalSaver.create(challenges::save, () -> Duration.ofMinutes(30));
    saver.start();

    ChallengeListeners.registerAll(this);
    new CommandChallenges(challenges);

    Registry<LeaderboardSource> sources = Leaderboards.getSources();

    for (StreakCategory value : StreakCategory.values()) {
      String key = value.name().toLowerCase();
      String currentKey = "streaks/current/" + key;
      String highestKey = "streaks/highest/" + key;
      sources.register(currentKey, new ChallengeStreakSource(value, challenges, false));
      sources.register(highestKey, new ChallengeStreakSource(value, challenges, true));
    }
  }

  @Override
  public void onDisable() {
    challenges.save();
  }

  public void load() {
    reloadConfig();
    challenges.load();
  }

  @Override
  public void reloadConfig() {
    this.pluginConfig = TomlConfigs.loadPluginConfig(this, ChallengeConfig.class);
  }
}
