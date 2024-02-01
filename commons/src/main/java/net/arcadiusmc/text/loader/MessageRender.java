package net.arcadiusmc.text.loader;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.forthecrown.grenadier.Grenadier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MessageRender extends ViewerAwareMessage {

  MessageRender addValue(@NotNull String key, @Nullable Object value);

  Component create(@Nullable Audience viewer);

  default CommandSyntaxException exception() {
    return exception(null);
  }

  default CommandSyntaxException exception(@Nullable Audience viewer) {
    return Grenadier.exceptions().create(create(viewer));
  }

  default CommandSyntaxException exceptionWithContext(ImmutableStringReader reader) {
    return exceptionWithContext(reader, null);
  }

  default CommandSyntaxException exceptionWithContext(
      ImmutableStringReader reader,
      Audience viewer
  ) {
    return Grenadier.exceptions().createWithContext(create(viewer), reader);
  }
}
