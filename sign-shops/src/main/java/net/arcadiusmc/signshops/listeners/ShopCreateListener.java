package net.arcadiusmc.signshops.listeners;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldguard.protection.flags.StateFlag;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.signshops.SExceptions;
import net.arcadiusmc.signshops.SMessages;
import net.arcadiusmc.signshops.SPermissions;
import net.arcadiusmc.signshops.ShopManager;
import net.arcadiusmc.signshops.ShopType;
import net.arcadiusmc.signshops.SignShop;
import net.arcadiusmc.signshops.SignShopFlags;
import net.arcadiusmc.signshops.SignShops;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.WgUtils;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopCreateListener implements Listener {

  public static final String REGEX_START = "(|-|=)(|\\[|<|\\()";

  // The order of the braces matters here apparently,
  // the longer ones must be first
  public static final String REGEX_END = "(]|\\)|>|)(=|-|)";

  public static final Pattern SELL_PATTERN
      = Pattern.compile(REGEX_START + "sell" + REGEX_END, CASE_INSENSITIVE);

  public static final Pattern BUY_PATTERN
      = Pattern.compile(REGEX_START + "buy" + REGEX_END, CASE_INSENSITIVE);

  private final ShopManager manager;

  public ShopCreateListener(ShopManager manager) {
    this.manager = manager;
  }

  @EventHandler(ignoreCancelled = true)
  public void onSignShopCreate(SignChangeEvent event1) {
    try {
      onShopCreateThrowing(event1);
    } catch (CommandSyntaxException exc) {
      Exceptions.handleSyntaxException(event1.getPlayer(), exc);
    }
  }

  private void onShopCreateThrowing(SignChangeEvent event)
      throws CommandSyntaxException
  {
    if (Text.toString(event.line(0)).isBlank()) {
      return;
    }

    Player player = event.getPlayer();

    String line0 = Text.plain(event.line(0)).trim();
    String line1 = Text.plain(event.line(1));
    String line2 = Text.plain(event.line(2));
    String line3 = Text.plain(event.line(3));

    ShopType shopType;

    if (SELL_PATTERN.matcher(line0).matches()) {
      shopType = ShopType.SELL;
    } else if (BUY_PATTERN.matcher(line0).matches()) {
      shopType = ShopType.BUY;
    } else {
      // Top line matches neither of the type patterns, therefor
      // the player is probably not even trying to make a shop sign
      return;
    }

    if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission(SPermissions.ADMIN)) {
      shopType = shopType.toAdmin();
    }

    // No price given
    if (line3.isBlank()) {
      throw SExceptions.noPrice(player);
    }

    Sign sign = (Sign) event.getBlock().getState();
    Location location = sign.getLocation();

    String lastLine = line3.toLowerCase()
        .replaceAll("\\D", "")
        .trim();

    // Parse price
    int price;
    try {
      price = Integer.parseInt(lastLine);
    } catch (Exception e) {
      throw SExceptions.noPrice(player);
    }

    // Make sure they don't exceed the max shop price
    if (price > manager.getPlugin().getShopConfig().maxPrice()) {
      throw SExceptions.shopMaxPrice(player);
    }

    // They must give at least one line of info about the shop
    if (line2.isBlank() && line1.isBlank()) {
      throw SExceptions.noDescription(player);
    }

    //WorldGuard flag check
    StateFlag.State flagState = WgUtils.getFlagValue(location, SignShopFlags.SHOP_CREATION);

    if (flagState == StateFlag.State.DENY && !player.hasPermission(SPermissions.ADMIN)) {
      throw SExceptions.shopNotAllowed(player);
    }

    //creates the sign shop
    SignShop shop = manager.createSignShop(location, shopType, price, player.getUniqueId());

    // Resize inventory if better value set
    SPermissions.SHOP_SIZE.getTier(player).ifPresent(shop::resizeInventory);

    //Opens the example item selection screen
    player.openInventory(SignShops.createExampleInventory());
    Events.register(new ExampleItemSelectionListener(player, shop));

    if (shopType == ShopType.BUY) {
      event.line(0, shopType.getUnStockedLabel());
    } else {
      event.line(0, shopType.getStockedLabel());
    }

    event.line(3, SignShops.priceLine(price, sign));
  }

  @RequiredArgsConstructor
  public static class ExampleItemSelectionListener implements Listener {

    private final Player player;
    private final SignShop shop;

    @EventHandler
    public void invClickEvent(InventoryClickEvent event) {
      if (event.getCurrentItem() == null
          || !event.getWhoClicked().equals(player)
      ) {
        return;
      }

      if (event.getCurrentItem().getType() == Material.BARRIER) {
        event.setCancelled(true);
      }
    }

    //sets the example item and adds the item(s) to the shop's inventory
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
      if (!event.getPlayer().equals(player)
          || event.getInventory().getType() != InventoryType.HOPPER
      ) {
        return;
      }

      Player player = (Player) event.getPlayer();

      Events.unregister(this);

      Inventory inv = event.getInventory();
      var shopInv = shop.getInventory();

      ItemStack item = inv.getContents()[SignShops.EXAMPLE_ITEM_SLOT];

      //If example item was not found: destroy shop and tell them why they failed
      if (ItemStacks.isEmpty(item)) {
        shop.destroy(false);
        player.sendMessage(SMessages.shopCreateFailed(player));

        return;
      }

      if (SignShops.isResellDisabled(item)) {
        shop.destroy(false);
        player.sendMessage(SMessages.nonResellableItem(player, item));

        return;
      }

      //Add the item to the inventory
      shop.setExampleItem(item);
      shopInv.addItem(item.clone());

      //Send the info message
      player.sendMessage(SMessages.createdShop(player, shop));

      player.playSound(
          player.getLocation(),
          Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
          SoundCategory.MASTER,
          1, 1
      );

      shop.update();
      shop.delayUnload();
    }
  }
}