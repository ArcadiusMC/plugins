package net.arcadiusmc.items;

import net.arcadiusmc.ItemGraveService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum SoulboundGraveFilter implements ItemGraveService.Filter {
  FILTER;

  @Override
  public boolean shouldRemain(@NotNull ItemStack item, @NotNull Player player) {
    if (ArcadiusEnchantments.SOULBOUND == null) {
      return false;
    }

    return item.containsEnchantment(ArcadiusEnchantments.SOULBOUND);
  }
}
