package net.arcadiusmc.titles;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Tiers {

  public static final Registry<Tier> REGISTRY = Registries.newRegistry();
  static final Comparator<Holder<Tier>> BY_PRIORITY
      = Comparator.comparingInt(value -> -value.getValue().getPriority());

  static {
    REGISTRY.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<Tier> value) {
        String permission = UserTitles.getTierPermission(value);

        if (!Strings.isNullOrEmpty(permission)) {
          Permissions.register(permission);
        }
      }

      @Override
      public void onUnregister(Holder<Tier> value) {

      }
    });
  }

  public static final Tier DEFAULT = new Tier(
      Component.text("Default"),
      Material.STONE,
      ImmutableList.of(),
      "default",
      Integer.MIN_VALUE,
      List.of(),
      true,
      false,
      false
  );

  public static final Holder<Tier> DEFAULT_HOLDER = REGISTRY.register("none", DEFAULT);
}
