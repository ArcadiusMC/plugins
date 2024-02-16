package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.punish.JailCell;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;

@CommandFile("commands/jail.gcn")
class CommandJail {
  static final String PERMISSION = PunishType.JAIL.getPermission().getName();

  void imprison(
      CommandSource source,
      @Argument("player") User target,
      @Argument("cell") Holder<JailCell> cell,
      @Argument(value = "reason", optional = true) String reason
  ) throws CommandSyntaxException {
    PunishCommands.ensureCanPunish(source, target, PunishType.JAIL);
    Punishments.punish(source, target, PunishType.JAIL, reason, cell.getKey(), null);
  }
}
