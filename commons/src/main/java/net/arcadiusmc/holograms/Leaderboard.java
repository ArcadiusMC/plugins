package net.arcadiusmc.holograms;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.PlayerMessage;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Leaderboard {

  int DEFAULT_MAX_SIZE = 10;

  boolean update();

  boolean kill();

  boolean spawn();

  boolean isSpawned();

  Holder<LeaderboardSource> getSource();

  void setSource(Holder<LeaderboardSource> source);

  @Nullable Location getLocation();

  void setLocation(@Nullable Location location);

  @Nullable
  PlayerMessage getFooter();

  void setFooter(@Nullable PlayerMessage footer);

  @Nullable
  PlayerMessage getHeader();

  void setHeader(@Nullable PlayerMessage header);

  @Nullable
  PlayerMessage getFormat();

  void setFormat(@Nullable PlayerMessage format);

  int getMaxEntries();

  void setMaxEntries(int maxEntries);

  boolean fillMissingSlots();

  void setFillMissingSlots(boolean fillMissingSlots);

  Order getOrder();

  void setOrder(@NotNull Order order);

  @Nullable
  IntRange getFilter();

  void setFilter(@Nullable IntRange filter);

  enum Order {
    ASCENDING,
    DESCENDING
  }
}
