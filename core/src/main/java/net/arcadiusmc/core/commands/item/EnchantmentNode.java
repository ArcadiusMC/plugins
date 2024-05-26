package net.arcadiusmc.core.commands.item;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class EnchantmentNode extends ItemModifierNode {

  private static final int MIN_LEVEL = 1;
  private static final String ENCHANTMENT_ARG = "enchantment";

  public EnchantmentNode() {
    super("enchant");
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<enchantment> [<level: number(1..)>]")
        .addInfo("Applies the <enchantment> to your held item")
        .addInfo("with [level]. If [level] is not given, then")
        .addInfo("a level of 1 is used.")
        .addInfo("If the item is a book, enchanting it turns it")
        .addInfo("into an enchanted book");

    factory.usage("clear")
        .addInfo("Clears all your held item's enchantments");
  }

  @Override
  public void create(LiteralArgumentBuilder<CommandSource> command) {
    command
        .then(literal("clear")
            .executes(c -> {
              CommandSource source = c.getSource();

              ItemStack item = getHeld(source);
              ItemMeta meta = item.getItemMeta();

              for (var e : meta.getEnchants().keySet()) {
                meta.removeEnchant(e);
              }

              item.setItemMeta(meta);

              source.sendMessage(ItemMessages.CLEARED_ENCHANTMENTS.renderText(source));
              return 0;
            })
        )

        .then(argument(ENCHANTMENT_ARG, ArgumentTypes.enchantment())
            .executes(c -> enchant(c, MIN_LEVEL))

            .then(argument("level", IntegerArgumentType.integer(MIN_LEVEL))
                .suggests((context, builder) -> {
                  var ench = getEnchantment(context);

                  if (ench.getMaxLevel() <= MIN_LEVEL) {
                    return Suggestions.empty();
                  }

                  var token = builder.getRemainingLowerCase();
                  for (int i = MIN_LEVEL; i <= ench.getMaxLevel(); i++) {
                    var suggestion = i + "";

                    if (Completions.matches(token, suggestion)) {
                      builder.suggest(suggestion);
                    }
                  }

                  return builder.buildFuture();
                })

                .executes(c -> {
                  var level = c.getArgument("level", Integer.class);
                  return enchant(c, level);
                })
            )

            .then(literal("remove")
                .executes(c -> {
                  CommandSource source = c.getSource();

                  ItemStack held = getHeld(source);
                  Enchantment enchantment = getEnchantment(c);

                  if (!held.containsEnchantment(enchantment)) {
                    throw Messages.MESSAGE_LIST.render("cmd.enchant.error.notFound")
                        .addValue("enchantment", enchantment.displayName(1))
                        .exception(source);
                  }

                  int level = held.removeEnchantment(enchantment);

                  source.sendSuccess(
                      ItemMessages.REMOVED_ENCHANTMENT.get()
                          .addValue("enchant", enchantment)
                          .addValue("level", level)
                          .addValue("enchantment", enchantment.displayName(level))
                          .create(source)
                  );
                  return 0;
                })
            )
        );
  }

  Enchantment getEnchantment(CommandContext<CommandSource> c) {
    return c.getArgument(ENCHANTMENT_ARG, Enchantment.class);
  }

  int enchant(CommandContext<CommandSource> c, int level) throws CommandSyntaxException {
    CommandSource source = c.getSource();

    ItemStack item = getHeld(source);
    Enchantment enchantment = getEnchantment(c);

    if (item.getType() == Material.BOOK) {
      item.setType(Material.ENCHANTED_BOOK);
    }

    if (item.getEnchantmentLevel(enchantment) >= level) {
      throw ItemMessages.ENCHANT_LEVEL_TOO_LOW.exception(source);
    }

    ItemMeta meta = item.getItemMeta();

    if (meta instanceof EnchantmentStorageMeta storageMeta) {
      storageMeta.addStoredEnchant(enchantment, level, true);
    } else {
      meta.addEnchant(enchantment, level, true);
    }

    item.setItemMeta(meta);

    source.sendSuccess(
        ItemMessages.ADDED_ENCHANTMENT.get()
            .addValue("enchant", enchantment)
            .addValue("level", level)
            .addValue("enchantment", enchantment.displayName(level))
            .create(source)
    );
    return 0;
  }
}