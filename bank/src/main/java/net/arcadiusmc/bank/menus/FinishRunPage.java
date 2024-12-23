package net.arcadiusmc.bank.menus;

import net.arcadiusmc.Loggers;
import net.arcadiusmc.bank.BankPlugin;
import net.arcadiusmc.bank.BankRun;
import net.arcadiusmc.bank.BankRun.RunState;
import net.arcadiusmc.dom.Document;
import net.arcadiusmc.dom.Element;
import net.arcadiusmc.dom.event.EventTypes;
import net.arcadiusmc.dom.event.MouseButton;
import net.arcadiusmc.dom.event.MouseEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class FinishRunPage {

  private static final Logger LOGGER = Loggers.getLogger();

  public static void onDomInitialize(Document document) {
    document.addEventListener(EventTypes.DOM_LOADED, event -> onPageLoaded(event.getDocument()));
  }

  private static void onPageLoaded(Document document) {
    Element button = document.getElementById("btn");
    if (button == null) {
      LOGGER.error("Failed to get button element");
      return;
    }

    BankPlugin plugin = JavaPlugin.getPlugin(BankPlugin.class);
    Player player = document.getView().getPlayer();

    BankRun bankRun = plugin.getSessionMap().get(player.getUniqueId());
    if (bankRun == null) {
      LOGGER.warn("Player {} is not in a bank run", player.getName());
      return;
    }

    button.addEventListener(EventTypes.CLICK, event -> {
      MouseEvent mev = (MouseEvent) event;
      if (mev.getButton() != MouseButton.RIGHT) {
        return;
      }

      if (bankRun.getState() == RunState.INACTIVE) {
        return;
      }

      bankRun.kick(false);
    });
  }
}
