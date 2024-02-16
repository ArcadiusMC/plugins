package net.arcadiusmc.punish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Vectors;
import org.bukkit.World;
import org.spongepowered.math.vector.Vector3d;

public record JailCell(Bounds3i cellArea, Vector3d center, World world) {

  public static final Codec<JailCell> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Bounds3i.CODEC.fieldOf("area").forGetter(JailCell::cellArea),
            Vectors.V3D_CODEC.fieldOf("center").forGetter(JailCell::center),
            ExtraCodecs.WORLD_CODEC.fieldOf("world").forGetter(JailCell::world)
        )
        .apply(instance, JailCell::new);
  });

}
