package net.arcadiusmc.usables.virtual;

import com.mojang.serialization.Codec;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.commands.UsableTriggerCommand.TriggerArgumentType;
import net.arcadiusmc.utils.io.ExtraCodecs;

public enum RegionAction {
  ON_REGION_ENTER,
  ON_REGION_EXIT,
  ON_REGION_ENTER_EXIT,
  ON_REGION_MOVE_INSIDE_OF;

  public static final Codec<RegionAction> CODEC
      = ExtraCodecs.enumCodec(RegionAction.class);

  RegionTriggerType type;

  static void registerAll(Registry<ObjectType<? extends Trigger>> r) {
    var triggers = UsablesPlugin.get().getTriggers();
    var argumentType = new TriggerArgumentType(triggers);

    for (var value : values()) {
      value.type = new RegionTriggerType(value, argumentType);
      r.register(value.name().toLowerCase(), value.type);
    }
  }
}
