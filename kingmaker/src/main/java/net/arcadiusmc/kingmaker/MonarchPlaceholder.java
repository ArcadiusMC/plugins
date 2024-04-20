package net.arcadiusmc.kingmaker;

import java.util.UUID;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class MonarchPlaceholder implements TextPlaceholder {

  private final Kingmaker kingmaker;

  public MonarchPlaceholder(Kingmaker kingmaker) {
    this.kingmaker = kingmaker;
  }

  @Override
  public @Nullable Component render(String match, PlaceholderContext render) {
    UUID monarchId = kingmaker.getMonarchId();

    if (monarchId == null) {
      return Messages.renderText("kingmaker.none", render.viewer());
    }

    User user = Users.get(monarchId);
    return user.displayName(render.viewer());
  }
}
