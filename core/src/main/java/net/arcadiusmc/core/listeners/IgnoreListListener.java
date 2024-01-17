package net.arcadiusmc.core.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Collection;
import java.util.Set;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.events.ChannelMessageEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.event.UserAfkEvent;
import net.arcadiusmc.utils.Audiences;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class IgnoreListListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onChannelMessage(ChannelMessageEvent event) {
    User user = event.getUserSource();

    if (user == null) {
      return;
    }

    Set<Audience> viewers = event.getTargets();

    if (viewers.isEmpty()) {
      return;
    }

    if (viewers.size() == 1
        && !event.isAnnouncement()
        && !user.hasPermission(CorePermissions.IGNORE_BYPASS)
    ) {
      Audience first = viewers.iterator().next();
      User target = Audiences.getUser(first);

      if (target != null) {
        boolean blocked = UserBlockList.testBlocked(
            user, target,
            Messages.BLOCKED_SENDER,
            Messages.BLOCKED_TARGET
        );

        if (blocked) {
          event.setCancelled(true);
        }

        return;
      }
    }

    filter(user, viewers);
  }

  @EventHandler(ignoreCancelled = true)
  public void onAsyncChat(AsyncChatEvent event) {
    filter(Users.get(event.getPlayer()), event.viewers());
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserAfk(UserAfkEvent event) {
    event.addFilter(user -> !UserBlockList.areBlocked(user, event.getUser()));
  }

  static void filter(User user, Collection<Audience> viewers) {
    if (user.hasPermission(CorePermissions.IGNORE_BYPASS)) {
      return;
    }

    viewers.removeIf(audience -> {
      User viewer = Audiences.getUser(audience);

      if (viewer == null) {
        return false;
      }

      return UserBlockList.areBlocked(viewer, user);
    });
  }
}
