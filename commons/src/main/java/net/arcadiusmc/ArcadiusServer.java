package net.arcadiusmc;

import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.MessageRenderer;
import net.arcadiusmc.user.User;
import net.arcadiusmc.command.settings.SettingsBook;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface ArcadiusServer {

  static ArcadiusServer server() {
    return ServiceInstances.getServer();
  }

  @NotNull
  Location getServerSpawn();

  void setServerSpawn(@NotNull Location serverSpawn);

  @NotNull
  SettingsBook<User> getGlobalSettingsBook();

  MessageRenderer getAnnouncementRenderer();

  void announce(ViewerAwareMessage message);

  default void announce(ComponentLike like) {
    announce(viewer -> Text.valueOf(like, viewer));
  }

  void registerLeaveListener(String id, LeaveCommandListener listener);

  void unregisterLeaveListener(String id);

  interface LeaveCommandListener {
    boolean onUse(User player);
  }
}