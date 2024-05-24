package net.arcadiusmc.items.guns;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.TagTypes;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.slf4j.Logger;

@Getter @Setter
public class GunParticle {

  private static final Logger LOGGER = Loggers.getLogger();

  private Particle particle;
  private DustOptions options;

  private boolean forced = false;
  private double extra = 0;
  private int count = 1;

  private double offsetX = 0;
  private double offsetY = 0;
  private double offsetZ = 0;

  public ParticleBuilder createBuilder() {
    ParticleBuilder builder = new ParticleBuilder(particle);

    builder.force(forced);
    builder.extra(extra);
    builder.count(count);
    builder.offset(offsetX, offsetY, offsetZ);

    if (options != null && particle == Particle.DUST) {
      builder.data(options);
    }

    return builder;
  }

  public boolean isErroneous() {
    return particle == null;
  }

  public void save(CompoundTag tag) {
    tag.putDouble("offset_x", offsetX);
    tag.putDouble("offset_y", offsetY);
    tag.putDouble("offset_z", offsetZ);

    tag.putDouble("extra", extra);
    tag.putInt("count", count);
    tag.putBoolean("force", forced);

    if (options != null) {
      CompoundTag optTag = BinaryTags.compoundTag();
      int argb = options.getColor().asARGB();

      optTag.putFloat("size", options.getSize());
      optTag.putInt("color_argb", argb);

      if (options instanceof DustTransition trans) {
        int transArgb = trans.getToColor().asARGB();
        optTag.putInt("to_color_argb", transArgb);
      }

      tag.put("options", optTag);
    }

    tag.putString("particle", particle.name().toLowerCase());
  }

  public void load(CompoundTag tag) {
    String particleName = tag.getString("particle");

    try {
      particle = Particle.valueOf(particleName.toUpperCase());
    } catch (IllegalArgumentException exc) {
      LOGGER.error("Failed to load particle '{}'", particleName, exc);
      particle = null;
    }

    offsetX = tag.getDouble("offset_x", 0);
    offsetY = tag.getDouble("offset_y", 0);
    offsetZ = tag.getDouble("offset_z", 0);

    extra = tag.getDouble("extra", 0);
    count = tag.getInt("count", 1);
    forced = tag.getBoolean("force", forced);

    if (tag.contains("options", TagTypes.compoundType())) {
      CompoundTag optTag = tag.getCompound("options");

      float size = optTag.getFloat("size", 1);

      int color = numberValue(optTag.get("color_argb"));
      Color colorObject = Color.fromARGB(color);

      if (optTag.contains("to_color_argb")) {
        int toColor = numberValue(optTag.get("to_color_argb"));
        Color toColorObject = Color.fromARGB(toColor);

        options = new DustTransition(colorObject, toColorObject, size);
      } else {
        options = new DustOptions(colorObject, size);
      }
    } else {
      options = null;
    }
  }

  private static int numberValue(BinaryTag tag) {
    if (tag.isString()) {
      String str = tag.toString();

      if (str.startsWith("0x")) {
        str = str.substring(2);
      } else if (str.startsWith("#")) {
        str = str.substring(1);
      }

      try {
        return Integer.parseUnsignedInt(str, 16);
      } catch (NumberFormatException exc) {
        LOGGER.error("Invalid hex color '{}'", tag.asString());
        return 0;
      }
    }

    if (!tag.isNumber()) {
      LOGGER.error("Color tag was not a number");
      return 0;
    }

    return tag.asNumber().intValue();
  }

}
