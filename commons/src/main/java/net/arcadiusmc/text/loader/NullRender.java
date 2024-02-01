package net.arcadiusmc.text.loader;

import java.util.Objects;
import net.arcadiusmc.Loggers;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

class NullRender implements MessageRender {

  private static final Logger LOGGER = Loggers.getLogger();

  private final String key;

  public NullRender(String key) {
    this.key = key;
  }

  @Override
  public MessageRender addValue(@NotNull String key, @Nullable Object value) {
    Objects.requireNonNull(key, "Null key");
    return this;
  }

  @Override
  public Component create(@Nullable Audience viewer) {
    LOGGER.error("Unknown message render attempted! id='{}'", key);
    return Component.text(key);
  }
}
