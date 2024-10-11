package net.arcadiusmc.dungeons.gen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import net.forthecrown.grenadier.types.BlockFilterArgument.Result;
import org.bukkit.Material;

public class BlockFilters {

  static final Codec<BlockFilterArgument.Result> CODEC = new Codec<>() {
    @Override
    public <T> DataResult<Pair<Result, T>> decode(DynamicOps<T> ops, T input) {
      return ops.getStringValue(input)
          .flatMap(s -> ExtraCodecs.safeParse(s, ArgumentTypes.blockFilter()))
          .map(result -> Pair.of(result, input));
    }

    @Override
    public <T> DataResult<T> encode(Result input, DynamicOps<T> ops, T prefix) {
      return DataResult.error(() -> "This cannot be saved");
    }
  };

  public static Result create(Material material) {
    return new MaterialBlockFilter(material);
  }
}
