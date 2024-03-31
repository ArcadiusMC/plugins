package net.arcadiusmc.holograms;

import org.bukkit.Color;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.spongepowered.math.vector.Vector3f;

public interface HologramMeta {

  void copyFrom(HologramMeta meta);

  void setScale(Vector3f scale);

  void setTranslation(Vector3f translation);

  void apply(TextDisplay display);

  Vector3f getScale();

  Vector3f getTranslation();

  Billboard getBillboard();

  TextAlignment getAlign();

  Brightness getBrightness();

  Color getBackgroundColor();

  float getYaw();

  float getPitch();

  boolean isShadowed();

  boolean isSeeThrough();

  int getLineWidth();

  byte getOpacity();

  void setBillboard(Billboard billboard);

  void setAlign(TextAlignment align);

  void setBrightness(Brightness brightness);

  void setBackgroundColor(Color backgroundColor);

  void setYaw(float yaw);

  void setPitch(float pitch);

  void setShadowed(boolean shadowed);

  void setSeeThrough(boolean seeThrough);

  void setLineWidth(int lineWidth);

  void setOpacity(byte opacity);
}
