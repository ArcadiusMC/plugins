package net.arcadiusmc.items;

import net.arcadiusmc.ItemGraveService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

enum PluginGraveFilter implements ItemGraveService.Filter {
  FILTER;

  @Override
  public boolean shouldRemain(@NotNull ItemStack item, @NotNull Player player) {
    return ItemTypes.getItem(item)
        .map(i -> i.getType().getValue().isPersistentBeyondDeath())
        .orElse(false);
  }
}
