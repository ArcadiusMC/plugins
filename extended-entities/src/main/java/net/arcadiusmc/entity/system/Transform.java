package net.arcadiusmc.entity.system;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3d;

@Getter
public class Transform implements Component {

  static final int FLAG_WORLD_CHANGED = 0x1;
  static final int FLAG_POS_CHANGED = 0x2;
  static final int FLAG_VEL_CHANGED = 0x4;
  static final int FLAG_ROT_CHANGED = 0x8;

  static final int NONE_CHANGED = 0;
  static final int LOCATION_CHANGED = FLAG_WORLD_CHANGED | FLAG_POS_CHANGED;

  World world;

  final Vector3d lastPosition = new Vector3d();
  final Vector3d position = new Vector3d();
  final Vector3d velocity = new Vector3d();

  float yaw = 0;
  float pitch = 0;

  transient int flags = NONE_CHANGED;

  public Transform() {

  }

  public Transform(Location location) {
    if (location == null) {
      return;
    }

    this.world = location.getWorld();
    this.position.x = location.x();
    this.position.y = location.y();
    this.position.z = location.z();
    this.yaw = location.getYaw();
    this.pitch = location.getPitch();
  }

  public void setWorld(World world) {
    this.world = world;
    this.flags |= FLAG_WORLD_CHANGED;
  }

  public void setPosition(Vector3d position) {
    this.position.set(position);
    this.flags |= FLAG_POS_CHANGED;
  }

  public void setVelocity(Vector3d velocity) {
    this.velocity.set(velocity);
    this.flags |= FLAG_VEL_CHANGED;
  }

  public void setYaw(float yaw) {
    this.yaw = yaw;
    this.flags |= FLAG_ROT_CHANGED;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
    this.flags |= FLAG_ROT_CHANGED;
  }
}
