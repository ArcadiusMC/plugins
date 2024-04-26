package net.arcadiusmc.merchants.commands;

import java.time.ZonedDateTime;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.merchants.Merchant;
import net.arcadiusmc.merchants.MerchantsPlugin;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandMerchants extends BaseCommand {

  private final MerchantsPlugin plugin;

  public CommandMerchants(MerchantsPlugin plugin) {
    super("merchants");
    this.plugin = plugin;

    setDescription("Merchants management command");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("reload-config")
            .executes(c -> {
              plugin.reloadConfig();
              c.getSource().sendSuccess(Messages.renderText("merchants.reloaded.config"));

              return 0;
            })
        )

        .then(literal("reload-data")
            .executes(c -> {
              plugin.reloadConfig();
              plugin.load();

              c.getSource().sendSuccess(Messages.renderText("merchants.reloaded.plugin"));
              return 0;
            })
        )

        .then(literal("save")
            .executes(c -> {
              plugin.save();
              c.getSource().sendSuccess(Messages.renderText("merchants.saved"));

              return 0;
            })
        )

        .then(literal("trigger-day-change")
            .executes(c -> {
              ZonedDateTime time = ZonedDateTime.now();
              for (Merchant merchant : plugin.getMerchants()) {
                merchant.onDayChange(time);
              }

              c.getSource().sendSuccess(Messages.renderText("merchants.dayChangeTriggered"));
              return 0;
            })
        );
  }
}
