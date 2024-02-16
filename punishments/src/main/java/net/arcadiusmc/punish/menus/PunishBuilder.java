package net.arcadiusmc.punish.menus;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishment;
import net.forthecrown.grenadier.CommandSource;

@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
class PunishBuilder {

  final PunishEntry entry;
  final PunishType type;

  String reason;
  String extra;
  Duration length;

  public void punish(CommandSource source) {
    Instant now = Instant.now();
    Instant ends = length == null ? null : now.plus(length);

    Punishment punishment = new Punishment(type, source.textName(), reason, extra, now, ends);

    entry.punish(punishment, source);
  }
}