package net.arcadiusmc.entity.util;

import java.util.UUID;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public enum UuidDataType implements PersistentDataType<int[], UUID> {
  INSTANCE;

  @Override
  public @NotNull Class<int[]> getPrimitiveType() {
    return int[].class;
  }

  @Override
  public @NotNull Class<UUID> getComplexType() {
    return UUID.class;
  }

  @Override
  public int @NotNull [] toPrimitive(
      @NotNull UUID complex,
      @NotNull PersistentDataAdapterContext context
  ) {
    return ExtraCodecs.uuidToIntArray(complex);
  }

  @Override
  public @NotNull UUID fromPrimitive(
      int @NotNull [] primitive,
      @NotNull PersistentDataAdapterContext context
  ) {
    return ExtraCodecs.uuidFromIntArray(primitive);
  }
}
