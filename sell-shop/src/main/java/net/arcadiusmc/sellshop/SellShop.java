package net.arcadiusmc.sellshop;

import java.nio.file.Path;
import java.util.List;
import java.util.Stack;
import lombok.Getter;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.sellshop.event.ItemPriceCalculateEvent;
import net.arcadiusmc.sellshop.loader.PageLoader;
import net.arcadiusmc.sellshop.loader.SellShopPage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import org.bukkit.Material;

public class SellShop {

  public static final ContextSet SET = ContextSet.create();
  public static final ContextOption<Stack<SellShopPage>> PAGE_STACK = SET.newOption();

  private final SellShopPlugin plugin;

  @Getter
  private final Registry<SellShopPage> pages;

  public SellShop(SellShopPlugin plugin) {
    this.plugin = plugin;
    this.pages = Registries.newRegistry();
  }

  public void load() {
    pages.clear();

    Path pagesDir = PathUtil.pluginPath(this.plugin, "pages");
    PluginJar.saveResources("pages", pagesDir);

    PageLoader loader = new PageLoader(pagesDir, this.pages, this.plugin.getDataSource());
    loader.load();
  }

  public static SellResult applyCalculationEvent(
      SellResult result,
      User user,
      List<String> tags,
      Material material
  ) {
    if (ItemPriceCalculateEvent.getHandlerList().getRegisteredListeners().length < 1) {
      return result;
    }

    ItemPriceCalculateEvent event = new ItemPriceCalculateEvent(user, tags, material);
    event.setEarned(result.getEarned());
    event.setSold(result.getSold());

    event.callEvent();

    return new SellResult(
        event.getSold(),
        event.getEarned(),
        result.getTarget(),
        result.getFailure()
    );
  }
}