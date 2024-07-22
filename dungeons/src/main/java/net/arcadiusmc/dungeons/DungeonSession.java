package net.arcadiusmc.dungeons;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

public class DungeonSession {

  @Getter
  private final SessionMap sessionMap;

  private final Set<Player> players = new HashSet<>();

  @Getter @Setter
  private DungeonStructure structure;

  @Getter @Setter
  private long cellId = 0;

  public DungeonSession(SessionMap sessionMap) {
    this.sessionMap = sessionMap;
  }

  public void addPlayer(Player player) {
    if (!players.add(player)) {
      return;
    }

    sessionMap.onAddPlayer(player.getUniqueId(), this);
  }

  public void removePlayer(Player player) {
    if (!players.remove(player)) {
      return;
    }

    sessionMap.onRemovePlayer(player.getUniqueId());
  }
}
