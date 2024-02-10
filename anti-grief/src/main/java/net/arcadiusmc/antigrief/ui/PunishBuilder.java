package net.arcadiusmc.antigrief.ui;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.antigrief.PunishEntry;
import net.arcadiusmc.antigrief.PunishType;
import net.arcadiusmc.antigrief.Punishments;
import net.forthecrown.grenadier.CommandSource;

@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class PunishBuilder {

  final PunishEntry entry;
  final PunishType type;

  String reason;
  String extra;
  Duration length;

  public void punish(CommandSource source) {
    Punishments.handlePunish(entry.getUser(), source, reason, length, type, extra);
  }
}