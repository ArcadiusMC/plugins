package net.arcadiusmc.core.commands;

import java.util.HashMap;
import java.util.Map;
import net.arcadiusmc.ArcadiusServer.LeaveCommandListener;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandLeave extends BaseCommand {

  public static final Map<String, LeaveCommandListener> listeners = new HashMap<>();

  public CommandLeave() {
    super("leave");

    setAliases("quit", "exit");
    setDescription("Use when you want to leave an event or server area");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      var user = getUserSender(c);

      for (LeaveCommandListener value : listeners.values()) {
        if (value.onUse(user)) {
          return 1;
        }
      }

      throw Exceptions.create("Not allowed to use here");
    });
  }
}
