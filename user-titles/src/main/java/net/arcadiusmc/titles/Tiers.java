package net.arcadiusmc.titles;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Tiers {

  public static final Registry<Tier> REGISTRY = Registries.newRegistry();

  public static final Tier DEFAULT = new Tier(
      Component.text("Default"),
      Material.STONE,
      ImmutableList.of(),
      "default",
      Integer.MIN_VALUE,
      List.of(),
      true,
      false
  );

  static {
    REGISTRY.register("none", DEFAULT);
  }
}
