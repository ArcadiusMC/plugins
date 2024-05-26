package net.arcadiusmc.utils.io;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

public interface JomlCodecs {

  Codec<Vector3d> VEC3D = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.DOUBLE.optionalFieldOf("x", 0d).forGetter(Vector3d::x),
            Codec.DOUBLE.optionalFieldOf("y", 0d).forGetter(Vector3d::y),
            Codec.DOUBLE.optionalFieldOf("z", 0d).forGetter(Vector3d::z)
        )
        .apply(instance, Vector3d::new);
  });

  Codec<Vector3f> VEC3F = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(Vector3f::x),
            Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(Vector3f::y),
            Codec.FLOAT.optionalFieldOf("z", 0f).forGetter(Vector3f::z)
        )
        .apply(instance, Vector3f::new);
  });

  Codec<Vector3i> VEC3I = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.INT.optionalFieldOf("x", 0).forGetter(Vector3i::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(Vector3i::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(Vector3i::z)
        )
        .apply(instance, Vector3i::new);
  });

  Codec<Vector2d> VEC2D = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.DOUBLE.optionalFieldOf("x", 0d).forGetter(Vector2d::x),
            Codec.DOUBLE.optionalFieldOf("y", 0d).forGetter(Vector2d::y)
        )
        .apply(instance, Vector2d::new);
  });

  Codec<Vector2f> VEC2F = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(Vector2f::x),
            Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(Vector2f::y)
        )
        .apply(instance, Vector2f::new);
  });

  Codec<Vector2i> VEC2I = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.INT.optionalFieldOf("x", 0).forGetter(Vector2i::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(Vector2i::y)
        )
        .apply(instance, Vector2i::new);
  });
}
