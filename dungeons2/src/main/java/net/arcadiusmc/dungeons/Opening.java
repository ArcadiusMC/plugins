package net.arcadiusmc.dungeons;

import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CompoundTag;

public record Opening(int width, int height) {

  static final Opening DEFAULT = new Opening(12, 21);

  public static Opening load(BinaryTag tag) {
    if (tag == null || !tag.isCompound()) {
      return DEFAULT;
    }

    CompoundTag c = tag.asCompound();
    return new Opening(c.getInt("width"), c.getInt("height"));
  }
}
