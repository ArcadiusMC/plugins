package net.arcadiusmc.core.user;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.user.Users;
import org.jetbrains.annotations.NotNull;

@Getter
public class PropertyImpl<T> implements UserProperty<T> {

  private final T defaultValue;
  private final PropertyEditCallback<T> callback;

  private final Codec<T> codec;
  private final ArgumentType<T> argumentType;

  int id = -1;
  String key;

  public PropertyImpl(
      T defaultValue,
      PropertyEditCallback<T> callback,
      Codec<T> codec,
      ArgumentType<T> type
  ) {
    this.defaultValue = defaultValue;
    this.callback = callback;
    this.codec = codec;
    this.argumentType = type;
  }

  @Setter @Accessors(chain = true, fluent = true)
  public static class BuilderImpl<T> implements Builder<T> {

    private String key;
    private PropertyEditCallback<T> callback;
    private T defaultValue;

    private final Codec<T> codec;
    private final ArgumentType<T> type;

    public BuilderImpl(Codec<T> codec, ArgumentType<T> type) {
      this.codec = codec;
      this.type = type;
    }

    public Builder<T> key(String key) {
      Registries.ensureValidKey(key);
      this.key = key;
      return this;
    }

    @Override
    public Builder<T> defaultValue(@NotNull T value) {
      Objects.requireNonNull(value);
      this.defaultValue = value;
      return this;
    }

    @Override
    public UserProperty<T> build() {
      Objects.requireNonNull(key, "No key set");
      Objects.requireNonNull(defaultValue, "No default value set");

      PropertyImpl<T> property = new PropertyImpl<>(defaultValue, callback, codec, type);
      Registry<UserProperty<?>> registry = Users.getService().getUserProperties();
      registry.register(key, property);
      return property;
    }
  }
}