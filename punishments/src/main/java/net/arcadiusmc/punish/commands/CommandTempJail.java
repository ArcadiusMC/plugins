package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import net.arcadiusmc.punish.JailCell;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;

@CommandFile("commands/tempjail.gcn")
public class CommandTempJail {
  static final String PERMISSION = PunishType.JAIL.getPermission().getName();

  void imprison(
      CommandSource source,
      @Argument("player") User target,
      @Argument("cell") Holder<JailCell> cell,
      @Argument("length") Duration length,
      @Argument(value = "reason", optional = true) String reason
  ) throws CommandSyntaxException {
    PunishCommands.ensureCanPunish(source, target, PunishType.JAIL);
    Punishments.punish(source, target, PunishType.JAIL, reason, cell.getKey(), length);
  }
}
