package net.arcadiusmc.cosmetics;

import net.arcadiusmc.cosmetics.command.Emotes;
import net.arcadiusmc.cosmetics.travel.TravelEffects;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;

public class Cosmetics {

  public static final Registry<CosmeticType<?>> TYPES = Registries.newFreezable();

  static {
    TYPES.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<CosmeticType<?>> value) {
        CosmeticType<?> type = value.getValue();

        type.id = value.getId();
        type.key = value.getKey();

        type.initPermissions();
      }

      @Override
      public void onUnregister(Holder<CosmeticType<?>> value) {
        CosmeticType<?> type = value.getValue();
        type.id = -1;
        type.key = null;
      }
    });
  }

  static void registerAll(CosmeticsConfig cfg) {
    TYPES.register("death", DeathEffects.createType(cfg));
    TYPES.register("arrow", ArrowEffects.createType(cfg));
    TYPES.register("travel", TravelEffects.createType(cfg));
    TYPES.register("emote", Emotes.createType());
    TYPES.freeze();
  }
}
