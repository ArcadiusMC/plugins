package net.arcadiusmc.core.user;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.user.UserFlags;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

public class Flags implements UserFlags {

  public static final Codec<Map<UUID, Set<String>>> CODEC
      = Codec.unboundedMap(ExtraCodecs.STRING_UUID, ExtraCodecs.set(Codec.STRING));

  @Getter
  private final Map<UUID, Set<String>> flags = new Object2ObjectOpenHashMap<>();

  @Getter @Setter
  private boolean dirty;

  @Override
  public boolean hasFlag(@NotNull UUID playerId, @NotNull String flag) {
    Objects.requireNonNull(playerId, "Null player id");
    Objects.requireNonNull(flag, "Null flag");

    Set<String> set = flags.get(playerId);
    if (set == null || set.isEmpty()) {
      return false;
    }

    return set.contains(flag);
  }

  @Override
  public boolean setFlag(@NotNull UUID playerId, @NotNull String flag) {
    Objects.requireNonNull(playerId, "Null player id");
    Objects.requireNonNull(flag, "Null flag");

    Set<String> set = flags.computeIfAbsent(playerId, k -> new ObjectOpenHashSet<>());

    if (set.add(flag)) {
      dirty = true;
      return true;
    }

    return false;
  }

  @Override
  public boolean unsetFlag(@NotNull UUID playerId, @NotNull String flag) {
    Objects.requireNonNull(playerId, "Null player id");
    Objects.requireNonNull(flag, "Null flag");

    Set<String> set = flags.get(playerId);
    if (set == null || set.isEmpty()) {
      return false;
    }

    if (set.remove(flag)) {
      dirty = true;
      return true;
    }

    return false;
  }

  @Override
  public Set<String> getFlags(@NotNull UUID playerId) {
    Objects.requireNonNull(playerId, "Null player id");

    Set<String> set = flags.get(playerId);
    if (set == null) {
      return ObjectSets.emptySet();
    }

    return Collections.unmodifiableSet(set);
  }
}
