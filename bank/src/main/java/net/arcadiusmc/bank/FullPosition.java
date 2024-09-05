package net.arcadiusmc.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FullPosition {

  static final Codec<FullPosition> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.FLOAT.optionalFieldOf("x", 0.0f).forGetter(FullPosition::getX),
            Codec.FLOAT.optionalFieldOf("y", 0.0f).forGetter(FullPosition::getY),
            Codec.FLOAT.optionalFieldOf("z", 0.0f).forGetter(FullPosition::getZ),

            Codec.FLOAT.optionalFieldOf("yaw", 0.0f).forGetter(FullPosition::getPitch),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0f).forGetter(FullPosition::getYaw)
        )

        .apply(instance, FullPosition::new);
  });

  private float x = 0;
  private float y = 0;
  private float z = 0;

  private float yaw = 0;
  private float pitch = 0;

  public void set(FullPosition other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;

    this.yaw = other.yaw;
    this.pitch = other.pitch;
  }

  public Location toLocation(World world) {
    return new Location(world, x, y, z, yaw, pitch);
  }
}
