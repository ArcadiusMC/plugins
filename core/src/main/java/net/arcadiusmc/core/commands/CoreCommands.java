package net.arcadiusmc.core.commands;

import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.CurrencyCommand;
import net.arcadiusmc.command.UserMapTopCommand;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.commands.admin.CommandAlts;
import net.arcadiusmc.core.commands.admin.CommandBroadcast;
import net.arcadiusmc.core.commands.admin.CommandCoinPile;
import net.arcadiusmc.core.commands.admin.CommandCooldown;
import net.arcadiusmc.core.commands.admin.CommandArcadiusCore;
import net.arcadiusmc.core.commands.admin.CommandGameMode;
import net.arcadiusmc.core.commands.admin.CommandGetOffset;
import net.arcadiusmc.core.commands.admin.CommandGetPos;
import net.arcadiusmc.core.commands.admin.CommandInvStore;
import net.arcadiusmc.core.commands.admin.CommandLaunch;
import net.arcadiusmc.core.commands.admin.CommandMemory;
import net.arcadiusmc.core.commands.admin.CommandPlayerTime;
import net.arcadiusmc.core.commands.admin.CommandSign;
import net.arcadiusmc.core.commands.admin.CommandSkull;
import net.arcadiusmc.core.commands.admin.CommandSmite;
import net.arcadiusmc.core.commands.admin.CommandSpecificGameMode;
import net.arcadiusmc.core.commands.admin.CommandSpeed;
import net.arcadiusmc.core.commands.admin.CommandSudo;
import net.arcadiusmc.core.commands.admin.CommandTab;
import net.arcadiusmc.core.commands.admin.CommandTeleport;
import net.arcadiusmc.core.commands.admin.CommandTeleportExact;
import net.arcadiusmc.core.commands.admin.CommandTellRawF;
import net.arcadiusmc.core.commands.admin.CommandTextToJson;
import net.arcadiusmc.core.commands.admin.CommandTime;
import net.arcadiusmc.core.commands.admin.CommandTimeFields;
import net.arcadiusmc.core.commands.admin.CommandTop;
import net.arcadiusmc.core.commands.admin.CommandUserFlags;
import net.arcadiusmc.core.commands.tools.CommandCloseInventory;
import net.arcadiusmc.core.commands.tools.CommandUserProperty;
import net.arcadiusmc.core.commands.admin.CommandVanish;
import net.arcadiusmc.core.commands.admin.CommandWorld;
import net.arcadiusmc.core.commands.docs.CommandDocGen;
import net.arcadiusmc.core.commands.help.CommandHelp;
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
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
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
    new CommandLaunch();
    new CommandInvStore();
    new CommandGetPos();
    new CommandGetOffset();
    new CommandGameMode();
    new CommandCooldown();
    new CommandBroadcast();
    new CommandDocGen();
    new CommandTellRawF();
    new CommandSudo();
    new CommandSmite();
    new CommandTextToJson();
    new CommandUserFlags(plugin);
    new CommandCoinPile();

    // Tool commands
    new CommandUserProperty(plugin);
    new CommandCloseInventory();

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
    ctx.registerCommand(new CommandVanish(plugin));
    ctx.registerCommand(new CommandArcadiusCore());
    ctx.registerCommand(new CommandTab(plugin));
    ctx.registerCommand(new CommandAlts());
    ctx.registerCommand(new CommandTimeFields());
  }

  static void createCurrencyCommands() {
    UserServiceImpl users = (UserServiceImpl) Users.getService();
    var currencies = users.getCurrencies();

    currencies.get("balances").ifPresent(currency -> {
      new CurrencyCommand("balance", currency, "bal", "bank", "cash", "money", "ebal");
    });
  }

  static void createMapTopCommands() {
    UserServiceImpl users = (UserServiceImpl) Users.getService();

    new UserMapTopCommand(
        "baltop",
        users.getBalances(),
        Messages::currency,
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
