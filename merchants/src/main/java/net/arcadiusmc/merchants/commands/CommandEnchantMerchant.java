package net.arcadiusmc.merchants.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.merchants.EnchantsMerchant;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandEnchantMerchant extends BaseCommand {

  final EnchantsMerchant merchant;

  public CommandEnchantMerchant(EnchantsMerchant merchant) {
    super("enchant-merchant");
    this.merchant = merchant;

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage(
        "show-earnings",
        "Shows how much the enchantment merchant has earned"
    );

    factory.usage(
        "select-new",
        "Forcefully select a new enchantment"
    );

    factory.usage(
        "clear-chosen",
        "Clears the list of enchantments the merchant is NOT allowed to select"
    );

  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("show-earnings")
            .executes(c -> {
              c.getSource().sendMessage(
                  Messages.render("merchants.enchants.earningsReport")
                      .addValue("weekly", merchant.getWeeklyEarnings())
                      .addValue("monthly", merchant.getMonthlyEarnings())
                      .addValue("lifetime", merchant.getLifetimeEarnings())
                      .create(c.getSource())
              );
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("clear-chosen")
            .executes(c -> {
              merchant.getAlreadyChosen().clear();

              c.getSource().sendSuccess(
                  Messages.render("merchants.enchants.clearedChosen")
                      .create(c.getSource())
              );
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("select-new")
            .executes(c -> {
              merchant.selectRandomEnchantment();

              c.getSource().sendSuccess(
                  Messages.render("merchants.enchants.selectedNew")
                      .create(c.getSource())
              );
              return SINGLE_SUCCESS;
            })
        );
  }
}
