package net.arcadiusmc.core.listeners;

import net.arcadiusmc.core.Coinpile;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class CoinpileListener implements Listener {

  static final Sound SOUND = Sound.sound()
      .type(Key.key("arcadiusmc:coin_jingle"))
      .build();

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    Entity entity = event.getRightClicked();
    PersistentDataContainer pdc = entity.getPersistentDataContainer();

    if (!pdc.has(Coinpile.WORTH, PersistentDataType.INTEGER)) {
      return;
    }

    int coinWorth = pdc.getOrDefault(Coinpile.WORTH, PersistentDataType.INTEGER, 0);
    if (coinWorth < 1) {
      return;
    }

    Player player = event.getPlayer();
    User user = Users.get(player);

    user.addBalance(coinWorth);
    user.playSound(SOUND);
    user.sendMessage(
        Messages.render("cmd.coinpile.pickedUp")
            .addValue("worth", Messages.currency(coinWorth))
            .create(user)
    );

    Entity vehicle = entity.getVehicle();
    if (vehicle != null) {
      vehicle.remove();
    }

    entity.remove();
    event.setCancelled(false);
  }
}
