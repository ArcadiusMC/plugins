package net.arcadiusmc.user.event;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UserLogRenderer {

  UserLogRenderer DEFAULT_JOIN = (user, viewer) -> Messages.joinMessage(user).create(viewer);

  static UserLogRenderer defaultLeave(QuitReason reason) {
    return (user, viewer) -> Messages.leaveMessage(user, reason).create(viewer);
  }

  static UserLogRenderer newNameJoin(String newName) {
    return (user, viewer) -> Messages.newNameJoinMessage(user, newName).create(viewer);
  }

  static UserLogRenderer firstJoin() {
    return (user, viewer) -> Messages.firstTimeJoin(user).create(viewer);
  }

  /**
   * Renders the login/logout message
   * @param user User that joined/left
   * @param viewer Viewer seeing the message
   * @return Rendered message, or {@code null}, to not show a message to the viewer
   */
  @Nullable
  Component render(@NotNull User user, @NotNull Audience viewer);
}