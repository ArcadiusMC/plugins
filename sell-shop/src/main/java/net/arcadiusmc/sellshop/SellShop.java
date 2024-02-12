package net.arcadiusmc.sellshop;

import java.nio.file.Path;
import java.util.Stack;
import lombok.Getter;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.sellshop.loader.PageLoader;
import net.arcadiusmc.sellshop.loader.SellShopPage;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;

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
}