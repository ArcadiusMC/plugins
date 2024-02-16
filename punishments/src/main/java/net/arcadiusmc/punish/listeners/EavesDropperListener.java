package net.arcadiusmc.punish.listeners;

import com.google.common.base.Strings;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.arcadiusmc.events.ChannelMessageEvent;
import net.arcadiusmc.punish.EavesDropper;
import net.arcadiusmc.punish.MuteState;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.user.Users;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

class EavesDropperListener implements Listener {

  // Strings support regex
  static final Map<String, UserProperty<Boolean>> VALID_CHANNELS = Map.ofEntries(
      Map.entry("commands/tell", EavesDropper.EAVES_DROP_DM),
      Map.entry("marriage_chat", EavesDropper.EAVES_DROP_MCHAT),
      Map.entry("afk",           EavesDropper.EAVES_DROP_MUTED)
  );

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChannelMessage(ChannelMessageEvent event) {
    if (event.isAnnouncement() || event.getSource() == null) {
      return;
    }

    UserProperty<Boolean> property = null;
    String channelName = event.getChannelName();

    if (Strings.isNullOrEmpty(channelName) || channelName.equals(ChannelMessageEvent.UNSET_NAME)) {
      return;
    }

    for (Entry<String, UserProperty<Boolean>> entry : VALID_CHANNELS.entrySet()) {
      if (!entry.getKey().equalsIgnoreCase(channelName)) {
        continue;
      }

      property = entry.getValue();
      break;
    }

    if (property == null) {
      return;
    }

    if (property == EavesDropper.EAVES_DROP_MUTED && !event.isCancelled()) {
      return;
    }

    EavesDropper.reportMessage(
        event.getSource(),
        event.getTargets(),
        property,
        event.getMessage(),
        event.getRenderer()
    );
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAsyncChat(AsyncChatEvent event) {
    User user = Users.get(event.getPlayer());
    MuteState mute = Punishments.getMute(user);

    if (mute == MuteState.NONE) {
      return;
    }

    EavesDropper.reportMessage(
        event.getPlayer(),
        Set.of(),
        EavesDropper.EAVES_DROP_MUTED,
        PlayerMessage.of(event.signedMessage().message(), user),
        (viewer, baseMessage) -> {
          return Messages.chatMessage(viewer, user, baseMessage);
        }
    );
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onSignChange(SignChangeEvent event) {
    var player = event.getPlayer();
    EavesDropper.reportSign(player, event.getBlock(), event.lines());
  }
}
