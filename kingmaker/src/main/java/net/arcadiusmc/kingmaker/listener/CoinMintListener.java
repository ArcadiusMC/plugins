package net.arcadiusmc.kingmaker.listener;

import java.util.UUID;
import net.arcadiusmc.events.CoinCreationEvent;
import net.arcadiusmc.kingmaker.Kingmaker;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CoinMintListener implements Listener {

  private final Kingmaker kingmaker;

  public CoinMintListener(Kingmaker kingmaker) {
    this.kingmaker = kingmaker;
  }

  @EventHandler(ignoreCancelled = true)
  public void onCoinCreation(CoinCreationEvent event) {
    UUID monarchId = kingmaker.getMonarchId();
    if (monarchId == null) {
      return;
    }

    User user = Users.get(monarchId);

    Component line = Messages.render("kingmaker.coinLore")
        .addValue("player", user.getName())
        .asComponent();

    event.getBuilder().addLore(line);
  }
}
