package net.arcadiusmc.votinghook;

import com.bencodez.votingplugin.topvoter.TopVoter;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.holograms.Holograms;
import net.arcadiusmc.holograms.LeaderboardSource;
import net.arcadiusmc.registry.Registry;
import org.bukkit.plugin.java.JavaPlugin;

public class VotingHookPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    Events.register(new VoteListener());

    Registry<LeaderboardSource> sources = Holograms.getSources();
    sources.register("votes/daily",   new VoteLeaderboardSource(TopVoter.Daily));
    sources.register("votes/monthly", new VoteLeaderboardSource(TopVoter.Monthly));
    sources.register("votes/weekly",  new VoteLeaderboardSource(TopVoter.Weekly));
    sources.register("votes/alltime", new VoteLeaderboardSource(TopVoter.AllTime));
  }
}
