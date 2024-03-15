package net.arcadiusmc.factions;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import net.arcadiusmc.utils.property.IdProperty;

@Getter
public class FactionProperty<T> implements IdProperty<T> {

  private final Codec<T> codec;
  private final ArgumentType<T> argumentType;
  private final Supplier<T> defaultValue;

  private final UpdateListener<T> listener;

  int id = -1;
  String key;

  @lombok.Builder(builderClassName = "Builder")
  public FactionProperty(
      Codec<T> codec,
      ArgumentType<T> argumentType,
      Supplier<T> defaultValue,
      UpdateListener<T> listener
  ) {
    Objects.requireNonNull(codec, "Null codec");
    Objects.requireNonNull(argumentType, "Null argument type");

    this.codec = codec;
    this.argumentType = argumentType;
    this.defaultValue = defaultValue;
    this.listener = listener;
  }

  public static <T> Builder<T> builder(Codec<T> codec) {
    Builder<T> builder = new Builder<>();
    return builder.codec(codec);
  }

  @Override
  public T getDefaultValue() {
    if (defaultValue == null) {
      return null;
    }
    return defaultValue.get();
  }

  public void onUpdate(Faction faction, T previousValue, T newValue) {
    if (listener == null) {
      return;
    }

    listener.onUpdate(faction, previousValue, newValue);
  }

  public interface UpdateListener<T> {

    void onUpdate(Faction faction, T previousValue, T newValue);
  }
}
