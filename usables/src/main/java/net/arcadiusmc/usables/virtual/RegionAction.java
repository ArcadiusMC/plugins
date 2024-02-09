package net.arcadiusmc.usables.virtual;

import com.mojang.serialization.Codec;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.commands.UsableTriggerCommand.TriggerArgumentType;
import net.arcadiusmc.utils.io.FtcCodecs;

public enum RegionAction {
  ON_REGION_ENTER,
  ON_REGION_EXIT,
  ON_REGION_ENTER_EXIT,
  ON_REGION_MOVE_INSIDE_OF;

  public static final Codec<RegionAction> CODEC
      = FtcCodecs.enumCodec(RegionAction.class);

  RegionTriggerType type;

  static {
    var triggers = UsablesPlugin.get().getTriggers();
    var argumentType = new TriggerArgumentType(triggers);

    for (RegionAction value : values()) {
      value.type = new RegionTriggerType(value, argumentType);
    }
  }

  static void registerAll(Registry<ObjectType<? extends Trigger>> r) {
    for (var value : values()) {
      r.register(value.name().toLowerCase(), value.type);
    }
  }
}
