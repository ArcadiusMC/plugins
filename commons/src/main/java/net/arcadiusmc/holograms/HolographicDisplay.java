package net.arcadiusmc.holograms;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public interface HolographicDisplay {

  boolean update();

  boolean kill();

  boolean spawn();

  boolean isSpawned();

  @Nullable Location getLocation();

  void setLocation(@Nullable Location location);

  Component renderText(@Nullable Audience viewer);

  Component displayName();

  HologramMeta getDisplayMeta();
}
