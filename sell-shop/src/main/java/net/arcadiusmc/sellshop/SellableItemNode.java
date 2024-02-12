package net.arcadiusmc.sellshop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.sellshop.data.ItemSellData;
import net.arcadiusmc.text.BufferedTextWriter;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public class SellableItemNode implements MenuNode {

  private final ItemSellData data;
  private final boolean autoSellToggleAllowed;

  @Override
  public void onClick(User user, Context ctx, ClickContext context)
      throws CommandSyntaxException
  {
    boolean compacted = user.get(SellProperties.COMPACTED) && data.canBeCompacted();
    var material = compacted ? data.getCompactMaterial() : data.getMaterial();

    // If toggling auto sell
    if (context.getClickType().isShiftClick()) {
      toggleAutoSell(user, material, context);
      return;
    }

    ItemSeller handler = ItemSeller.inventorySell(user, material, data);
    var result = handler.run(true);

    if (result.getFailure() == null) {
      context.shouldReloadMenu(true);
    }
  }

  private void toggleAutoSell(User user, Material material, ClickContext context)
      throws CommandSyntaxException
  {
    if (!autoSellToggleAllowed) {
      return;
    }

    if (!user.hasPermission(SellPermissions.AUTO_SELL)) {
      throw Messages.render("sellshop.autoSell.noPermission").exception(user);
    }

    var autoSelling = user.getComponent(UserShopData.class)
        .getAutoSelling();

    if (autoSelling.contains(material)) {
      autoSelling.remove(material);
    } else {
      autoSelling.add(material);
    }

    context.shouldReloadMenu(true);
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    boolean compacted = user.get(SellProperties.COMPACTED) && data.canBeCompacted();

    UserShopData earnings = user.getComponent(UserShopData.class);
    Material material = compacted ? data.getCompactMaterial() : data.getMaterial();

    int amount = user.get(SellProperties.SELL_AMOUNT).getItemAmount();
    int mod = compacted ? data.getCompactMultiplier() : 1;
    int originalPrice = mod * data.getPrice();

    int price = ItemSell.calculateValue(material, data, earnings, 1).getEarned();

    BufferedTextWriter writer = TextWriters.buffered();

    writer.line(
        Messages.render("sellshop.item.value")
            .addValue("price", Messages.currency(price))
    );

    if (originalPrice < price) {
      writer.line(
          Messages.render("sellshop.item.originalValue")
              .addValue("originalPrice", originalPrice)
      );
    }

    addStackSellInfo(writer, material, earnings);
    addPriceChangeInfo(writer, material, earnings);

    writer.newLine();
    writer.newLine();

    writer.line(
        Messages.render("sellshop.sellAmount.info")
            .addValue("sellAmount", user.get(SellProperties.SELL_AMOUNT).amountText())
    );

    writer.line(
        Messages.render("sellshop.sellAmount.change")
    );

    DefaultItemBuilder builder = ItemStacks.builder(material)
        .setAmount(amount);

    if (user.hasPermission(SellPermissions.AUTO_SELL) && autoSellToggleAllowed) {
      if (earnings.getAutoSelling().contains(material)) {
        builder
            .addEnchant(Enchantment.BINDING_CURSE, 1)
            .setFlags(ItemFlag.HIDE_ENCHANTS);

        writer.line(Messages.render("sellshop.autoSell.prompt.off"));
      } else {
        writer.line(Messages.render("sellshop.autoSell.prompt.on"));
      }
    }

    builder.setLore(writer.getBuffer());
    return builder.build();
  }

  private void addStackSellInfo(
      BufferedTextWriter writer,
      Material material,
      UserShopData earnings
  ) {
    SellResult stackResult = ItemSell.calculateValue(
        material, this.data, earnings, material.getMaxStackSize()
    );

    writer.line(
        Messages.render("sellshop.item.valuePerStack")
            .addValue("stackSize", material.getMaxStackSize())
            .addValue("valuePerStack", stackResult.getEarned())
    );

    if (stackResult.getSold() < material.getMaxStackSize()) {
      writer.line(
          Messages.render("sellshop.item.limitedSellsLeft")
              .addValue("until0", stackResult.getSold())
      );
    }
  }

  private void addPriceChangeInfo(
      BufferedTextWriter writer,
      Material material,
      UserShopData earnings
  ) {
    int earned = earnings.get(material);
    int rhinesUntilDrop = data.calcPriceDrop(earned);
    int price = data.calculatePrice(earned);

    if (price <= 0) {
      return;
    }

    int itemsUntilPriceDrop = rhinesUntilDrop / price;

    itemsUntilPriceDrop = Math.max(1, itemsUntilPriceDrop);

    writer.line(
        Messages.render("sellshop.item.priceWillDrop")
            .addValue("itemsUntilDrop", itemsUntilPriceDrop)
    );
  }
}