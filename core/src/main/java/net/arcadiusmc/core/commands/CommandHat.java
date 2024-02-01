package net.arcadiusmc.core.commands;

import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.sound.Sound;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CommandHat extends BaseCommand {

  public CommandHat() {
    super("Hat");

    setPermission(CorePermissions.HAT);
    setDescription("Places the item in your hand on your head");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /Hat
   *
   * Permissions used:
   *
   * Main Author:
   */

  private static final Sound SOUND = Sound.sound(
      org.bukkit.Sound.ITEM_ARMOR_EQUIP_NETHERITE,
      Sound.Source.PLAYER,
      1f, 1f
  );

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          Player player = c.getSource().asPlayer();
          ItemStack held = Commands.getHeldItem(player);

          PlayerInventory inventory = player.getInventory();
          ItemStack helmet = inventory.getHelmet();

          if (ItemStacks.notEmpty(helmet)
              && helmet.containsEnchantment(Enchantment.BINDING_CURSE)
              && player.getGameMode() != GameMode.CREATIVE
          ) {
            throw CoreExceptions.CANNOT_REMOVE_HELMET.exception(player);
          }

          inventory.setHelmet(held);
          inventory.setItemInMainHand(helmet);

          player.playSound(SOUND);
          return 0;
        });
  }
}