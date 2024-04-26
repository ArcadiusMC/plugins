package net.arcadiusmc.merchants.listeners;

import lombok.RequiredArgsConstructor;
import net.arcadiusmc.events.DayChangeEvent;
import net.arcadiusmc.merchants.Merchant;
import net.arcadiusmc.merchants.MerchantsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class DayChangeListener implements Listener {

  private final MerchantsPlugin plugin;

  @EventHandler(ignoreCancelled = true)
  public void onDayChange(DayChangeEvent event) {
    for (Merchant merchant : plugin.getMerchants()) {
      merchant.onDayChange(event.getTime());
    }
  }
}
