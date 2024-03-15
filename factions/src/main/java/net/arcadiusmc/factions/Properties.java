package net.arcadiusmc.factions;

import static net.arcadiusmc.factions.FactionsDiscord.NULL_ID;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;

public final class Properties {
  private Properties() {}

  public static final Registry<FactionProperty<?>> REGISTRY;

  static {
    REGISTRY = Registries.newFreezable();

    REGISTRY.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<FactionProperty<?>> value) {
        FactionProperty<?> prop = value.getValue();
        prop.id = value.getId();
        prop.key = value.getKey();
      }

      @Override
      public void onUnregister(Holder<FactionProperty<?>> value) {
        FactionProperty<?> prop = value.getValue();
        prop.id = -1;
        prop.key = null;
      }
    });
  }

  static final FactionProperty<Component> DISPLAY_NAME = textProperty().build();

  static final FactionProperty<String> LP_GROUP = stringProperty().build();

  static final FactionProperty<Long> CHANNEL_ID = longProperty()
      .defaultValue(() -> NULL_ID)
      .build();

  static final FactionProperty<Long> ROLE_ID = longProperty()
      .defaultValue(() -> NULL_ID)
      .build();

  static void registerAll() {
    REGISTRY.register("display_name", DISPLAY_NAME);
    REGISTRY.register("lp_group", LP_GROUP);
    REGISTRY.register("text_channel_id", CHANNEL_ID);
    REGISTRY.register("discord_role_id", ROLE_ID);

    REGISTRY.freeze();
  }

  private static FactionProperty.Builder<Long> longProperty() {
    return FactionProperty.builder(Codec.LONG)
        .argumentType(LongArgumentType.longArg());
  }

  private static FactionProperty.Builder<String> stringProperty() {
    return FactionProperty.builder(Codec.STRING).argumentType(StringArgumentType.greedyString());
  }

  private static FactionProperty.Builder<Component> textProperty() {
    return FactionProperty.builder(ExtraCodecs.COMPONENT)
        .argumentType(ArgumentTypes.map(Arguments.CHAT, ViewerAwareMessage::asComponent));
  }
}
