package net.arcadiusmc.punish;

import java.util.List;
import java.util.Set;
import net.arcadiusmc.command.settings.BookSetting;
import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.channel.MessageRenderer;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.utils.Audiences;
import net.arcadiusmc.utils.math.WorldVec3i;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class EavesDropper {
  private EavesDropper() {}

  /**
   * Determines if a user sees a muted user's chat messages with {@link EavesDropper}
   * <p>
   * Only affects users with {@link GPermissions#EAVESDROP}
   */
  public static final UserProperty<Boolean> EAVES_DROP_MUTED
      = Properties.booleanProperty("eavesDrop/muted", false);

  /**
   * Determines if a user sees other people's direct messages through Eavesdropper
   * <p>
   * Only affects users with {@link GPermissions#EAVESDROP}
   */
  public static final UserProperty<Boolean> EAVES_DROP_DM
      = Properties.booleanProperty("eavesDrop/dm", false);

  /**
   * Determines if a user sees what people write on signs with EavesDropper
   * <p>
   * Only affects users with {@link GPermissions#EAVESDROP}
   */
  public static final UserProperty<Boolean> EAVES_DROP_SIGN
      = Properties.booleanProperty("eavesDrop/signs", false);

  /**
   * Determines if a user sees what other married users send to each other through marriage chat
   * with EavesDropper
   * <p>
   * Only affects users with {@link GPermissions#EAVESDROP}
   */
  public static final UserProperty<Boolean> EAVES_DROP_MCHAT
      = Properties.booleanProperty("eavesDrop/mchat", false);

  /**
   * Determines if a user sees eaves dropper messages when other users mine into veins
   * <p>
   * Only affects uses with {@link GPermissions#EAVESDROP}
   */
  public static final UserProperty<Boolean> EAVES_DROP_MINING
      = Properties.booleanProperty("eavesDrop/mining", false);

  static void createSettings(SettingsBook<User> settingsBook) {
    Setting muted = Setting.create(EAVES_DROP_MUTED)
        .setMessageKey("settings.spyMuted")
        .setPermission(GPermissions.EAVESDROP);

    Setting dms = Setting.create(EAVES_DROP_DM)
        .setMessageKey("settings.spyDms")
        .setPermission(GPermissions.EAVESDROP);

    Setting signs = Setting.create(EAVES_DROP_SIGN)
        .setMessageKey("settings.spySigns")
        .setPermission(GPermissions.EAVESDROP);

    Setting mchat = Setting.create(EAVES_DROP_MCHAT)
        .setMessageKey("settings.spyMchat")
        .setPermission(GPermissions.EAVESDROP);

    Setting veins = Setting.create(EAVES_DROP_MINING)
        .setMessageKey("settings.miningSpy")
        .setPermission(GPermissions.EAVESDROP);

    List<BookSetting<User>> settings = settingsBook.getSettings();
    settings.add(muted.toBookSettng());
    settings.add(dms.toBookSettng());
    settings.add(signs.toBookSettng());
    settings.add(mchat.toBookSettng());
    settings.add(veins.toBookSettng());
  }

  public static void reportMessage(
      Audience source,
      Set<Audience> targets,
      UserProperty<Boolean> viewProperty,
      ViewerAwareMessage message,
      MessageRenderer renderer
  ) {
    ChannelledMessage ch = createChannelled(viewProperty, viewer -> {
      return renderer.render(viewer, message.create(viewer));
    });

    MuteState state = Punishments.getMute(source);
    if (state != MuteState.NONE) {
      MessageRenderer existing = ch.getRenderer();

      Component mutePrefix = switch (state) {
        case HARD -> Messages.renderText("eavesDropper.prefix.mute", null);
        case SOFT -> Messages.renderText("eavesDropper.prefix.softmute", null);
        default -> throw new IllegalArgumentException();
      };

      ch.setRenderer((viewer, baseMessage) -> {
        return existing.render(viewer, Component.textOfChildren(mutePrefix, baseMessage));
      });
    }

    ch.filterTargets(audience -> {
      return !Audiences.equals(source, audience) && !Audiences.contains(audience, targets);
    });

    ch.send();
  }

  private static ChannelledMessage createChannelled(
      UserProperty<Boolean> viewProperty,
      ViewerAwareMessage message
  ) {
    ChannelledMessage ch = ChannelledMessage.create(message);
    ch.setChannelName("eaves_dropper");
    ch.addTargets(Bukkit.getOnlinePlayers());
    ch.addTarget(Bukkit.getConsoleSender());

    ch.setRenderer((viewer, baseMessage) -> {
      return Messages.render("eavesDropper.format")
          .addValue("message", baseMessage)
          .create(viewer);
    });

    ch.filterTargets(audience -> {
      User user = Audiences.getUser(audience);

      if (user == null) {
        return true;
      }

      if (!user.hasPermission(GPermissions.EAVESDROP)) {
        return false;
      }

      return user.get(viewProperty);
    });

    return ch;
  }

  public static void reportSign(Player player, Block block, List<Component> lines) {
    if (isEmpty(lines) || player.hasPermission(GPermissions.EAVESDROP_ADMIN)) {
      return;
    }

    var pos = WorldVec3i.of(block);

    var ch = createChannelled(EAVES_DROP_SIGN, viewer -> {
      var formatted = Text.vformat(
          """
          &7{0, user} placed a sign at {1, location, -world -clickable}:
          &71)&r {2}
          &72)&r {3}
          &73)&r {4}
          &74)&r {5}""",
          player,
          pos,
          lines.get(0),
          lines.get(1),
          lines.get(2),
          lines.get(3)
      );

      return formatted.create(viewer);
    });

    // Remove player from the list
    ch.filterTargets(audience -> !Audiences.equals(audience, player));
    ch.send();
  }

  private static boolean isEmpty(List<Component> lines) {
    if (lines.isEmpty()) {
      return true;
    }

    for (var c : lines) {
      if (!Text.plain(c).isBlank()) {
        return false;
      }
    }

    return true;
  }

  public static void reportOreMining(Block block, int veinSize, Player player) {
    ChannelledMessage ch = createChannelled(EAVES_DROP_MINING, viewer -> {
      return Text.format(
          "&e{0, user}&r found &e{1, number} {2}&r at &e{3, location, -clickable -world}&r.",
          NamedTextColor.GRAY,
          player,
          veinSize,
          block.getType(),
          block.getLocation()
      );
    });

    // Remove player from the list
    ch.filterTargets(audience -> !Audiences.equals(audience, player));
    ch.send();
  }
}
