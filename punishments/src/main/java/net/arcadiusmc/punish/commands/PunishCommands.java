package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Map;
import java.util.Optional;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.punish.GExceptions;
import net.arcadiusmc.punish.GMessages;
import net.arcadiusmc.punish.GPermissions;
import net.arcadiusmc.punish.JailCell;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.PunishPlugin;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishment;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;

public class PunishCommands {

  static final RegistryArguments<JailCell> CELL_ARG
      = new RegistryArguments<>(Punishments.getJails().getCells(), "Jail cell");

  public static void ensureCanPardon(CommandSource source, User target, PunishType type)
      throws CommandSyntaxException
  {
    if (source.hasPermission(GPermissions.ADMIN)) {
      return;
    }

    if (!source.hasPermission(type.getPermission())) {
      throw GExceptions.cannotPardon(source, target, type);
    }

    boolean alreadyPunished = Punishments.getOptionalEntry(target)
        .map(entry -> entry.isPunished(type))
        .orElse(false);

    if (!alreadyPunished) {
      return;
    }

    throw GExceptions.alreadyPunished(source, target, type);
  }

  public static void ensureCanPunish(CommandSource source, User target, PunishType type)
      throws CommandSyntaxException
  {
    if (!source.hasPermission(type.getPermission())) {
      throw Exceptions.create(GMessages.noPunishmentPermission(source, type));
    }

    if (!Punishments.canPunish(source, target)) {
      throw GExceptions.cannotPunish(source, target);
    }

    Optional<PunishEntry> opt = Punishments.getOptionalEntry(target);

    if (opt.isEmpty()) {
      return;
    }

    PunishEntry entry = opt.get();
    Punishment punishment = entry.getCurrent(type);

    if (punishment == null) {
      return;
    }

    throw GExceptions.alreadyPunished(source, target, type);
  }

  public static void registerAll(PunishPlugin plugin) {
    new CommandSeparate();
    new CommandPunish();
    new CommandJails(plugin.getJails());

    // Temporary punishment commands
    TempPunishCommand tempSoftmute = new TempPunishCommand("tempsoftmute", PunishType.SOFTMUTE);
    tempSoftmute.setDescription("Temporarily softmutes a player");
    tempSoftmute.register();

    TempPunishCommand tempmute = new TempPunishCommand("tempmute", PunishType.MUTE);
    tempmute.setDescription("Temporarily mutes a player");
    tempmute.register();

    TempPunishCommand tempban = new TempPunishCommand("tempban", PunishType.BAN);
    tempban.setDescription("Temporarily bans a player");
    tempban.register();

    TempPunishCommand tempIpban = new TempPunishCommand("tempipban", PunishType.IPBAN);
    tempIpban.setDescription("Temporarily IP-bans a player");
    tempIpban.register();

    // Permanent punishment commands
    PermPunishCommand softmute = new PermPunishCommand("softmute", PunishType.SOFTMUTE);
    softmute.setDescription("Permanently softmutes a player");
    softmute.register();

    PermPunishCommand mute = new PermPunishCommand("mute", PunishType.MUTE);
    mute.setDescription("Permanently mutes a player");
    mute.register();

    PermPunishCommand ban = new PermPunishCommand("ban", PunishType.BAN);
    ban.setDescription("Permanently bans a player");
    ban.register();

    PermPunishCommand ipban = new PermPunishCommand("ipban", PunishType.IPBAN);
    ipban.setDescription("Permanently IP-bans a player");
    ipban.register();

    // Pardon commands
    PardonCommand pardonSoftmute = new PardonCommand("pardon-softmute", PunishType.SOFTMUTE);
    pardonSoftmute.setAliases("pardonsoftmute", "unsoftmute");
    pardonSoftmute.setDescription("Pardons a player's softmute");
    pardonSoftmute.register();

    PardonCommand pardonMute = new PardonCommand("pardon-mute", PunishType.MUTE);
    pardonMute.setAliases("pardonmute", "unmute");
    pardonMute.setDescription("Pardons a player's mute");
    pardonMute.register();

    PardonCommand pardonBan = new PardonCommand("pardon-ban", PunishType.BAN);
    pardonBan.setAliases("pardonban", "unban");
    pardonBan.setDescription("Unbans a player");
    pardonBan.register();

    PardonCommand pardonIpBan = new PardonCommand("pardon-ipban", PunishType.IPBAN);
    pardonIpBan.setAliases("unipban", "pardonipban");
    pardonIpBan.setDescription("Pardon's a player's IP ban");
    pardonIpBan.register();

    PardonCommand pardonJail = new PardonCommand("pardon-jail", PunishType.JAIL);
    pardonJail.setAliases("unjail", "pardonjail");
    pardonJail.setDescription("Pardon's a player jail sentence");
    pardonJail.register();

    AnnotatedCommandContext context = Commands.createAnnotationContext();

    Map<String, Object> vars = context.getVariables();
    vars.put("cell_arg", CELL_ARG);

    context.registerCommand(new CommandJail());
    context.registerCommand(new CommandTempJail());
    context.registerCommand(new CommandKick());
    context.registerCommand(new CommandNotes(plugin));
  }
}
