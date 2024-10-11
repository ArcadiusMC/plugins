package net.arcadiusmc.dungeons.gen;

import java.util.Map;
import java.util.Set;
import net.forthecrown.grenadier.types.BlockFilterArgument.Result;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MaterialBlockFilter(Material material) implements Result {

  @Override
  public @NotNull Set<Material> getMaterials() {
    return Set.of(material);
  }

  @Override
  public @Nullable CompoundTag getTag() {
    return null;
  }

  @Override
  public @NotNull Map<String, String> getParsedProperties() {
    return Map.of();
  }

  @Override
  public boolean test(BlockState state) {
    return state.getType() == material;
  }

  @Override
  public boolean test(BlockData data) {
    return data.getMaterial() == material;
  }
}
