package net.arcadiusmc.punish;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.punish.event.PunishmentExpireEvent;
import net.arcadiusmc.punish.event.UserPardonedEvent;
import net.arcadiusmc.punish.event.UserPunishedEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.audience.Audience;
import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class PunishEntry {

  static final PunishType[] TYPES = PunishType.values();

  public static final Codec<PunishEntry> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.UUID_CODEC.fieldOf("player_id")
                .forGetter(PunishEntry::getPlayerId),

            Punishment.CODEC.listOf().optionalFieldOf("past", List.of())
                .forGetter(PunishEntry::getPast),

            Note.CODEC.listOf().optionalFieldOf("notes", List.of())
                .forGetter(PunishEntry::getNotes),

            Punishment.CODEC.listOf()
                .comapFlatMap(
                    punishments -> {
                      Punishment[] map = new Punishment[TYPES.length];
                      for (Punishment punishment : punishments) {
                        int id = punishment.getType().ordinal();
                        Punishment existing = map[id];

                        if (existing != null) {
                          return Results.error("Active %s defined twice", punishment.getType());
                        }

                        map[id] = punishment;
                      }

                      return Results.success(map);
                    },
                    punishments -> {
                      return Arrays.stream(punishments)
                          .filter(Objects::nonNull)
                          .toList();
                    }
                )
                .optionalFieldOf("current", new Punishment[0])
                .forGetter(o -> o.current)
        )
        .apply(instance, (uuid, past, notes, current) -> {
          PunishEntry entry = new PunishEntry(uuid);

          if (current.length > 0) {
            System.arraycopy(current, 0, entry.current, 0, current.length);
          }

          entry.notes.addAll(notes);
          entry.past.addAll(past);

          return entry;
        });
  });

  public static final Codec<Map<UUID, PunishEntry>> ENTRIES = PunishEntry.CODEC.listOf()
      .comapFlatMap(
          punishEntries -> {
            Map<UUID, PunishEntry> entryMap = new HashMap<>();

            for (PunishEntry punishEntry : punishEntries) {
              if (punishEntry.isEmpty()) {
                continue;
              }

              UUID playerId = punishEntry.getPlayerId();

              if (entryMap.containsKey(playerId)) {
                return Results.error("ID '%s' has more than 1 entry", playerId);
              }

              entryMap.put(playerId, punishEntry);
            }

            return Results.success(entryMap);
          },
          map -> new ArrayList<>(map.values())
      )
      .fieldOf("data")
      .codec();

  private final Punishment[] current = new Punishment[TYPES.length];
  private final BukkitTask[] expiryTasks = new BukkitTask[TYPES.length];

  @Getter
  private final List<Punishment> past = new ArrayList<>();

  @Getter
  private final List<Note> notes = new ArrayList<>();

  @Getter
  private final UUID playerId;

  /** Is this entry currently registered */
  private boolean validEntry = false;

  public PunishEntry(UUID playerId) {
    this.playerId = playerId;
  }

  private boolean isEmpty() {
    for (Punishment punishment : current) {
      if (punishment != null) {
        return false;
      }
    }

    return notes.isEmpty() && past.isEmpty();
  }

  void validate() {
    validEntry = true;

    for (Punishment punishment : current) {
      if (punishment == null) {
        continue;
      }

      setCurrent(punishment);
    }
  }

  void invalidate() {
    validEntry = false;

    for (BukkitTask expiryTask : expiryTasks) {
      if (expiryTask == null) {
        continue;
      }
      Tasks.cancel(expiryTask);
    }
  }

  public User getUser() {
    return Users.get(playerId);
  }

  public boolean isPunished(PunishType type) {
    Punishment active = getCurrent(type);

    if (active != null) {
      return true;
    }

    return switch (type) {
      case IPBAN -> {
        IpBanList list = Bukkit.getBanList(Type.IP);
        User user = Users.get(playerId);
        yield list.isBanned(user.getIp());
      }

      case BAN -> {
        ProfileBanList list = Bukkit.getBanList(Type.PROFILE);
        User user = Users.get(playerId);
        yield list.isBanned(user.getProfile());
      }

      default -> false;
    };
  }

  public List<Punishment> getCurrent() {
    List<Punishment> list = new ArrayList<>(current.length);

    for (Punishment punishment : current) {
      if (punishment == null) {
        continue;
      }
      list.add(punishment);
    }

    return list;
  }

  public Punishment getCurrent(PunishType type) {
    return current[type.ordinal()];
  }

  public boolean punish(Punishment punishment, @Nullable Audience source) {
    Objects.requireNonNull(punishment, "Null punishment");

    PunishType type = punishment.getType();
    Punishment existing = getCurrent(punishment.getType());

    if (existing != null) {
      return false;
    }

    setCurrent(punishment);

    if (validEntry) {
      User user = Users.get(playerId);

      boolean hasReason = !Strings.isNullOrEmpty(punishment.getReason());
      MessageRender broadcastFormat = Messages.render(type.getPunishAnnounceFormat(hasReason))
          .addValue("player", user)
          .addValue("reason", Text.valueOf(punishment.getReason()))
          .addValue("punished", type.namedEndingEd());

      Punishments.announce(broadcastFormat, source);

      UserPunishedEvent event = new UserPunishedEvent(user, punishment);
      event.callEvent();

      type.onPunishmentBegin(user, punishment);
    }

    return true;
  }

  private void setCurrent(Punishment punishment) {
    Objects.requireNonNull(punishment, "Null punishment");

    PunishType type = punishment.getType();
    int id = type.ordinal();

    if (type == PunishType.KICK) {
      past.add(punishment);
      return;
    }

    Instant expires = punishment.getExpires();
    Instant now = Instant.now();

    if (expires != null && now.isAfter(expires)) {
      if (validEntry) {
        endPunishment(type, null);
      } else {
        past.add(punishment);
      }

      return;
    }

    current[id] = punishment;

    if (expires != null && validEntry) {
      Duration until = Duration.between(now, expires);
      expiryTasks[id] = Tasks.runLater(new ExpiryTask(type), until);
    }
  }

  public boolean pardon(PunishType type, CommandSource source) {
    Objects.requireNonNull(type, "Null type");
    Objects.requireNonNull(source, "Null pardon source");
    return endPunishment(type, source);
  }

  private boolean endPunishment(PunishType type, @Nullable CommandSource source) {
    int id = type.ordinal();

    Punishment punishment = current[id];
    BukkitTask task = expiryTasks[id];

    Tasks.cancel(task);
    expiryTasks[id] = null;

    if (punishment == null) {
      if ((type == PunishType.BAN || type == PunishType.IPBAN) && validEntry) {
        User user = getUser();
        type.onPunishmentEnd(user, null);
        return true;
      }

      return false;
    }

    if (source != null) {
      punishment.setPardonDate(Instant.now());
      punishment.setPardonSource(source.textName());
    }

    current[id] = null;
    past.add(punishment);

    if (validEntry) {
      User user = Users.get(playerId);

      boolean wasPardoned = source != null;
      PunishConfig config = PunishPlugin.plugin().getPluginConfig();

      if (wasPardoned || config.announcePunishmentExpirations()) {
        MessageRender render = wasPardoned
            ? Messages.render("punishments.pardoned")
            : Messages.render("punishments.expired");

        render.addValue("player", user)
            .addValue("punishment", type.presentableName());

        Punishments.announce(render, source);
      }

      Event event = wasPardoned
          ? new UserPardonedEvent(user, punishment, source)
          : new PunishmentExpireEvent(user, punishment);

      event.callEvent();
      type.onPunishmentEnd(user, punishment);
    }

    return true;
  }

  class ExpiryTask implements Runnable {

    private final PunishType type;

    public ExpiryTask(PunishType type) {
      this.type = type;
    }

    @Override
    public void run() {
      endPunishment(type, null);
    }
  }
}
