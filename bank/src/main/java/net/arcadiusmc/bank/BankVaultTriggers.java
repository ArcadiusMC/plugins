package net.arcadiusmc.bank;

import java.util.Map.Entry;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.delphi.Delphi;
import net.arcadiusmc.delphi.DelphiProvider;
import net.arcadiusmc.delphi.DocumentView;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

public class BankVaultTriggers {

  private static final Logger LOGGER = Loggers.getLogger();

  public static void onBankEnter(World world, Player player) {
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    Tasks.runLater(() -> {
      BankPlugin plugin = BankPlugin.getPlugin();
      Delphi delphi = DelphiProvider.get();

      for (Entry<String, BankVault> entry : plugin.getVaultMap().entrySet()) {
        BankVault vault = entry.getValue();
        FullPosition position = vault.getMenuEnterPosition();

        if (position.isNullLocation()) {
          continue;
        }

        String path = "bank-page:enter.xml?vault=" + entry.getKey();

        delphi.openDocument(path, player)
            .ifError(e -> {
              LOGGER.error("Failed to open vault enter page", e);
            })
            .ifSuccess(view -> {
              view.moveTo(position.toLocation(world));
            });
      }
    }, 20);
  }

  public static void onBankExit(Player player) {
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    Delphi delphi = DelphiProvider.get();

    delphi.getOpenViews(player)
        .stream()
        .filter(view -> view.getPath().toString().startsWith("bank-page:enter.xml"))
        .forEach(DocumentView::close);
  }
}
