package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;

@CommandFile("commands/kick.gcn")
class CommandKick {
  static final String PERMISSION = PunishType.KICK.getPermission().getName();

  void kick(
      CommandSource source,
      @Argument("player") User target,
      @Argument(value = "reason", optional = true) String reason
  ) throws CommandSyntaxException {
    PunishCommands.ensureCanPunish(source, target, PunishType.KICK);
    Punishments.punish(source, target, PunishType.KICK, reason, null, null);
  }
}
