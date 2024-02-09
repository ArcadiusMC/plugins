package net.arcadiusmc.antigrief.listeners;

import com.google.common.base.Strings;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import net.arcadiusmc.antigrief.EavesDropper;
import net.arcadiusmc.antigrief.Mute;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.events.ChannelMessageEvent;
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
  static final Map<Pattern, UserProperty<Boolean>> VALID_CHANNELS = Map.ofEntries(
      Map.entry(Pattern.compile("commands/tell"), EavesDropper.EAVES_DROP_DM),
      Map.entry(Pattern.compile("guild_chat/.+"), EavesDropper.EAVES_DROP_GUILD_CHAT),
      Map.entry(Pattern.compile("marriage_chat"), EavesDropper.EAVES_DROP_MCHAT),
      Map.entry(Pattern.compile("afk"),           EavesDropper.EAVES_DROP_MUTED)
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

    for (Entry<Pattern, UserProperty<Boolean>> entry : VALID_CHANNELS.entrySet()) {
      if (!entry.getKey().matcher(channelName).matches()) {
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
    Mute mute = Punishments.muteStatus(user);

    if (mute == Mute.NONE) {
      return;
    }

    EavesDropper.reportMessage(
        event.getPlayer(),
        Set.of(),
        EavesDropper.EAVES_DROP_MUTED,
        PlayerMessage.of(event.signedMessage().message(), user),
        (viewer, baseMessage) -> {
          return Messages.chatMessage(user.displayName(viewer), baseMessage);
        }
    );
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onSignChange(SignChangeEvent event) {
    var player = event.getPlayer();
    EavesDropper.reportSign(player, event.getBlock(), event.lines());
  }
}
