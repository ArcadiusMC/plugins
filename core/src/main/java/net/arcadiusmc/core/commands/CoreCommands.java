package net.arcadiusmc.core.commands;

import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.CurrencyCommand;
import net.arcadiusmc.command.UserMapTopCommand;
import net.arcadiusmc.core.commands.admin.CommandAlts;
import net.arcadiusmc.core.commands.admin.CommandBroadcast;
import net.arcadiusmc.core.commands.admin.CommandCooldown;
import net.arcadiusmc.core.commands.admin.CommandFtcCore;
import net.arcadiusmc.core.commands.admin.CommandGameMode;
import net.arcadiusmc.core.commands.admin.CommandGetOffset;
import net.arcadiusmc.core.commands.admin.CommandGetPos;
import net.arcadiusmc.core.commands.admin.CommandHologram;
import net.arcadiusmc.core.commands.admin.CommandInvStore;
import net.arcadiusmc.core.commands.admin.CommandLaunch;
import net.arcadiusmc.core.commands.admin.CommandMakeAward;
import net.arcadiusmc.core.commands.admin.CommandMemory;
import net.arcadiusmc.core.commands.admin.CommandPlayerTime;
import net.arcadiusmc.core.commands.admin.CommandSign;
import net.arcadiusmc.core.commands.admin.CommandSkull;
import net.arcadiusmc.core.commands.admin.CommandSpecificGameMode;
import net.arcadiusmc.core.commands.admin.CommandSpeed;
import net.arcadiusmc.core.commands.admin.CommandTab;
import net.arcadiusmc.core.commands.admin.CommandTeleport;
import net.arcadiusmc.core.commands.admin.CommandTeleportExact;
import net.arcadiusmc.core.commands.admin.CommandTellRawF;
import net.arcadiusmc.core.commands.admin.CommandTime;
import net.arcadiusmc.core.commands.admin.CommandTimeFields;
import net.arcadiusmc.core.commands.admin.CommandTop;
import net.arcadiusmc.core.commands.admin.CommandVanish;
import net.arcadiusmc.core.commands.admin.CommandWorld;
import net.arcadiusmc.core.commands.docs.CommandDocGen;
import net.arcadiusmc.core.commands.home.CommandDeleteHome;
import net.arcadiusmc.core.commands.home.CommandHome;
import net.arcadiusmc.core.commands.home.CommandHomeList;
import net.arcadiusmc.core.commands.home.CommandSetHome;
import net.arcadiusmc.core.commands.item.ItemModCommands;
import net.arcadiusmc.core.commands.tpa.CommandTpDeny;
import net.arcadiusmc.core.commands.tpa.CommandTpDenyAll;
import net.arcadiusmc.core.commands.tpa.CommandTpaAccept;
import net.arcadiusmc.core.commands.tpa.CommandTpaCancel;
import net.arcadiusmc.core.commands.tpa.CommandTpask;
import net.arcadiusmc.core.commands.tpa.CommandTpaskHere;
import net.arcadiusmc.core.user.UserServiceImpl;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.currency.Currency;
import net.kyori.adventure.text.Component;

public final class CoreCommands {
  private CoreCommands() {}

  public static void createCommands(CorePlugin plugin) {
    new CommandHelp();

    new CommandProfile();

    // Tpa
    new CommandTpaAccept();
    new CommandTpaCancel();
    new CommandTpask();
    new CommandTpaskHere();
    new CommandTpaCancel();
    new CommandTpDeny();
    new CommandTpDenyAll();

    // Admin commands
    new CommandWorld();
    new CommandTop();
    new CommandTime();
    new CommandTeleportExact();
    new CommandSpeed();
    new CommandSkull();
    new CommandSign();
    new CommandPlayerTime();
    new CommandMemory();
    new CommandMakeAward();
    new CommandLaunch();
    new CommandInvStore();
    new CommandHologram();
    new CommandGetPos();
    new CommandGetOffset();
    new CommandGameMode();
    new CommandCooldown();
    new CommandBroadcast();
    new CommandDocGen();
    new CommandTellRawF();

    new CommandSay();
    new CommandNickname();
    new CommandNear();
    new CommandMe();
    new CommandList();
    new CommandHat();
    new CommandBack();
    new CommandSettings();
    new CommandTell();
    new CommandReply();
    new CommandSuicide();
    new CommandRoll();
    new CommandWild(plugin.getWild());

    new CommandWithdraw();
    new CommandDeposit();
    new CommandPay();

    new CommandHome();
    new CommandHomeList();
    new CommandDeleteHome();
    new CommandSetHome();

    new CommandLeave();

    CommandSelfOrUser.createCommands();
    CommandDumbThing.createCommands();
    CommandSpecificGameMode.createCommands();
    ToolBlockCommands.createCommands();
    ItemModCommands.createCommands();

    createMapTopCommands();
    createCurrencyCommands();

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new CommandTeleport());
    ctx.registerCommand(new CommandVanish());
    ctx.registerCommand(new CommandFtcCore());
    ctx.registerCommand(new CommandTab());
    ctx.registerCommand(new CommandAlts());
    ctx.registerCommand(new CommandTimeFields());
  }

  static void createCurrencyCommands() {
    UserServiceImpl users = (UserServiceImpl) Users.getService();
    var currencies = users.getCurrencies();

    currencies.get("rhines").ifPresent(currency -> {
      new CurrencyCommand("balance", currency, "bal", "bank", "cash", "money", "ebal");
    });

    currencies.get("gems").ifPresent(currency -> {
      new CurrencyCommand("gems", currency);
    });

    new CurrencyCommand("votes", Currency.wrap("Vote", users.getVotes()));
  }

  static void createMapTopCommands() {
    UserServiceImpl users = (UserServiceImpl) Users.getService();

    new UserMapTopCommand(
        "baltop",
        users.getBalances(),
        UnitFormat::currency,
        Component.text("Top balances"),
        "balancetop", "banktop", "topbals", "topbalances"
    );

    new UserMapTopCommand(
        "topvoters",
        users.getVotes(),
        UnitFormat::votes,
        Component.text("Top voters"),
        "votetop"
    );

    new UserMapTopCommand(
        "playtimetop",
        users.getPlaytime(),
        UnitFormat::playTime,
        Component.text("Top by playtime"),
        "nolifetop", "topplayers"
    );
  }
}
