package net.arcadiusmc.user.name;

import net.arcadiusmc.user.User;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Element within a name
 */
public interface NameElement {

  /**
   * Writes this element's data to the specified writer
   *
   * @param user    User whose name is being formatted
   * @param context Display context
   */
  @Nullable
  Component createDisplay(User user, DisplayContext context);
}
