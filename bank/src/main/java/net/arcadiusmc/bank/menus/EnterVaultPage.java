package net.arcadiusmc.bank.menus;

import static net.arcadiusmc.bank.BankPlugin.ENTER_VAULT_IN_USE;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.bank.BankPlugin;
import net.arcadiusmc.bank.BankVault;
import net.arcadiusmc.dom.Document;
import net.arcadiusmc.dom.Element;
import net.arcadiusmc.dom.TagNames;
import net.arcadiusmc.dom.event.EventListener;
import net.arcadiusmc.dom.event.EventTypes;
import net.arcadiusmc.dom.event.MouseButton;
import net.arcadiusmc.dom.event.MouseEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.inventory.PlayerInventory;
import org.slf4j.Logger;

public class EnterVaultPage {

  private static final Logger LOGGER = Loggers.getLogger();

  public static void onDomInitialize(Document document) {
    document.addEventListener(EventTypes.DOM_LOADED, event -> onDomLoaded(document));
  }

  public static void onDomLoaded(Document document) {
    Element element = document.getElementById("enter-button");
    if (element == null) {
      return;
    }

    String vaultKey = document.getView().getPath().getQuery("vault");
    if (vaultKey == null) {
      return;
    }

    BankPlugin plugin = BankPlugin.getPlugin();
    BankVault vault = plugin.getVaultMap().get(vaultKey);

    if (vault == null) {
      return;
    }

    //appendVariations(document, vault);

    element.addEventListener(EventTypes.CLICK, event -> {
      MouseEvent mev = (MouseEvent) event;
      if (mev.getButton() != MouseButton.RIGHT) {
        return;
      }

      User user = Users.get(event.getDocument().getView().getPlayer());
      PlayerInventory inventory = user.getInventory();

      ItemList tickets = ItemLists.fromInventory(inventory, itemStack -> {
        if (ItemStacks.isEmpty(itemStack)) {
          return false;
        }

        CompoundTag customData = ItemStacks.getUnhandledTags(itemStack.getItemMeta());
        String ticketVault = customData.getString("bank_vault_key");

        if (Strings.isNullOrEmpty(ticketVault)) {
          return false;
        }
        return Objects.equals(vaultKey, ticketVault);
      });

      if (tickets.totalItemCount() < 1) {
        user.sendMessage(Messages.renderText("bankruns.ticketRequired", user));
        return;
      }

      int res = plugin.startRun(user, vault, vaultKey, "default");

      if (res == ENTER_VAULT_IN_USE) {
        user.sendMessage(Messages.renderText("bankruns.vaultInUse", user));
        return;
      }

      tickets.removeItems(1);
    });
  }

  public static void appendVariations(Document document, BankVault vault) {
    Element element = document.getElementById("variant-container");
    if (element == null) {
      return;
    }

    Set<String> variantSet = vault.getTable().getVariantTable().rowKeySet();

    List<String> variantList = new ArrayList<>(variantSet);
    variantList.sort(Comparator.naturalOrder());

    for (String s : variantList) {
      String displayName = vault.getVariationNames().getOrDefault(s, s);

      Element button = document.createElement(TagNames.BUTTON);
      button.setTextContent(displayName);

      element.appendChild(button);
    }
  }

  class Variant implements EventListener.Typed<MouseEvent> {

    private final String vaultKey;
    private final String variantKey;

    public Variant(String vaultKey, String variantKey) {
      this.vaultKey = vaultKey;
      this.variantKey = variantKey;
    }

    @Override
    public void handleEvent(MouseEvent event) {

    }
  }
}
