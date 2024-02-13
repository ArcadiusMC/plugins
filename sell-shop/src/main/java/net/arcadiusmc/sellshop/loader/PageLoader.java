package net.arcadiusmc.sellshop.loader;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Stack;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.sellshop.SellShop;
import net.arcadiusmc.sellshop.SellShopNodes;
import net.arcadiusmc.sellshop.data.ItemDataSource;
import net.arcadiusmc.sellshop.data.ItemSellData;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.Results;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;

public class PageLoader {

  private static final String ITEMS = "items";

  private static final Logger LOGGER = Loggers.getLogger();

  private static final int UNSET_SIZE = -1;

  private final Path loadDirectory;

  private final Registry<SellShopPage> pages;

  private final Map<String, LoadingPage> loadedPages = new HashMap<>();
  private final Map<String, MenuNode> hardcoded = new HashMap<>();

  private final Codec<List<Pair<Slot, Node>>> listCodec;

  private final ItemDataSource dataSource;

  public PageLoader(
      Path loadDirectory,
      Registry<SellShopPage> pages,
      ItemDataSource source
  ) {
    this.dataSource = source;

    this.loadDirectory = loadDirectory;

    this.pages = pages;

    hardcoded.put("filter_named",    SellShopNodes.SELLING_NAMED);
    hardcoded.put("filter_lore",     SellShopNodes.SELLING_LORE);
    hardcoded.put("toggle_compact",  SellShopNodes.COMPACT_TOGGLE);
    hardcoded.put("sell_amount_1",   SellShopNodes.SELL_PER_1);
    hardcoded.put("sell_amount_16",  SellShopNodes.SELL_PER_16);
    hardcoded.put("sell_amount_64",  SellShopNodes.SELL_PER_64);
    hardcoded.put("sell_amount_all", SellShopNodes.SELL_PER_ALL);

    listCodec = itemListCodec(this);
  }

  public void load() {
    // Load
    PathUtil.iterateDirectory(loadDirectory, true, true, this::loadPage);

    // Link
    resolveReferences();

    // Compile
    compileLoaded();
  }

  private void compileLoaded() {
    loadedPages.forEach((s, page) -> {
      if (page.abstractPage || page.size == UNSET_SIZE) {
        return;
      }

      SellShopPage built = page.build(this);
      pages.register(s, built);
    });
  }

  private void resolveReferences() {
    Iterator<Entry<String, LoadingPage>> it = loadedPages.entrySet().iterator();

    while (it.hasNext()) {
      Entry<String, LoadingPage> entry = it.next();
      LoadingPage page = entry.getValue();

      if (!Strings.isNullOrEmpty(page.extendsName)) {
        LoadingPage parent = loadedPages.get(page.extendsName);

        if (parent == null) {
          LOGGER.error("Cannot load page '{}': No menu '{}' found for 'extends' value",
              entry.getKey(), page.extendsName
          );

          it.remove();
          continue;
        }

        page.extend(parent);
        LOGGER.debug("Page '{}' is extending '{}'", entry.getKey(), page.extendsName);
      }

      if (page.size == UNSET_SIZE) {
        LOGGER.error("Page '{}' has unset size", entry.getKey());

        it.remove();
        continue;
      }

      if (page.headerItem == null && !page.abstractPage) {
        LOGGER.warn("Page '{}' has an unset header", entry.getKey());
      }

      boolean failed = false;

      for (int i = 0; i < page.nodes.length; i++) {
        Node n = page.nodes[i];

        if (n == null) {
          continue;
        }

        if (i >= page.size) {
          LOGGER.error("Node at slot {} doesn't fit inside menu with size {} ({} rows)",
              Slot.of(i),
              page.size,
              page.size / Slot.X_SIZE
          );
          failed = true;
        }

        if (n instanceof OpenMenuNode node) {
          LoadingPage target = loadedPages.get(node.menuName);
          if (target == null) {
            LOGGER.error("{}: open-menu node at slot {} references non-existant menu '{}'",
                entry.getKey(),
                Slot.of(i),
                node.menuName
            );

            failed = true;
            continue;
          }

          if (target.abstractPage) {
            LOGGER.error("{}: open-menu node at slot {} references template-only menu '{}'",
                entry.getKey(),
                Slot.of(i),
                node.menuName
            );

            failed = true;
            continue;
          }
        }


      }

      if (failed) {
        it.remove();
      }
    }
  }

  private String getKey(Path path) {
    return PathUtil.getFileKey(loadDirectory, path);
  }

  /* --------------------------- LOADING ---------------------------- */

  private void loadPage(Path path) throws IOException {
    JsonObject json = SerializationHelper.readAsJson(path);

    Optional<LoadingPage> pageOpt = SellShopCodecs.PAGE_CODEC.parse(JsonOps.INSTANCE, json)
        .mapError(s -> "Failed to load page '" + path + "': " + s)
        .resultOrPartial(LOGGER::error);

    if (pageOpt.isEmpty()) {
      return;
    }

    LoadingPage page = pageOpt.get();
    String key = getKey(path);
    page.ownKey = key;

    if (!loadItemValues(json, page)) {
      return;
    }

    if (key.equals("example")) {
      LOGGER.debug("Found 'example' file... skipping");
      return;
    }

    loadedPages.put(key, page);

    LOGGER.debug("Loaded page '{}'", key);
  }

  private boolean loadItemValues(JsonObject json, LoadingPage page) {
    String key = page.ownKey;

    if (!json.has(ITEMS)) {
      return true;
    }

    JsonElement items = json.get(ITEMS);

    Optional<List<Pair<Slot, Node>>> opt = listCodec.parse(JsonOps.INSTANCE, items)
        .mapError(s -> {
          return "Failed to load '" + ITEMS + "' from shop file '" + key + "': \n- "
              + s.replace(";", "\n-");
        })
        .resultOrPartial(LOGGER::error);

    if (opt.isEmpty()) {
      return false;
    }

    List<Pair<Slot, Node>> list = opt.get();

    for (Pair<Slot, Node> slotNodePair : list) {
      attemptNodeSet(page, slotNodePair.getFirst(), slotNodePair.getSecond());
    }

    return true;
  }

  private void attemptNodeSet(LoadingPage page, Slot slot, Node node) {
    int index = slot.getIndex();

    Node existing = page.nodes[index];
    if (existing != null) {
      LOGGER.warn("{}: Overwriting existing node at slot {}", page.ownKey, slot);
    }

    page.nodes[index] = node;
  }

  /* --------------------------- CODECS ---------------------------- */

  private static Codec<List<Pair<Slot, Node>>> itemListCodec(PageLoader loader) {
    Codec<Node> nodeCodec = createNodeCodec(loader);
    Codec<Pair<Slot, Node>> pairCodec = Codec.pair(Slot.CODEC.fieldOf("slot").codec(), nodeCodec);

    return pairCodec.listOf();
  }

  private static Codec<Node> createNodeCodec(PageLoader loader) {
    Codec<DirectNode> hardcodedCodec = ExtraCodecs.KEY_CODEC
        .flatXmap(
            s -> {
              MenuNode node = loader.hardcoded.get(s);

              if (node == null) {
                return Results.error("Unknown hardcoded node");
              }

              return Results.success(new DirectNode(node));
            },
            directNode -> {
              for (Entry<String, MenuNode> entry : loader.hardcoded.entrySet()) {
                if (entry.getValue() != directNode.node) {
                  continue;
                }

                return Results.success(entry.getKey());
              }

              return Results.error("Unknown node");
            }
        )
        .fieldOf("hardcoded")
        .codec();

    Codec<OpenMenuNode> openMenu = ExtraCodecs.KEY_CODEC
        .xmap(OpenMenuNode::new, OpenMenuNode::menuName)
        .fieldOf("open-menu")
        .codec();

    Codec<SellableNode> sellableNodeCodec = loader.dataSource.codec()
        .xmap(pair -> new SellableNode(pair.data(), pair.global()), sellableNode -> null)
        .fieldOf("sell")
        .codec();

    @SuppressWarnings({"rawtypes", "unchecked"})
    List<Codec<Node>> codecs
        = (List) List.of(openMenu, sellableNodeCodec, hardcodedCodec, ItemNode.CODEC);

    return new Codec<>() {
      @Override
      public <T> DataResult<Pair<Node, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<Node, T>> errors = null;

        for (Codec<Node> codec : codecs) {
          var result = codec.decode(ops, input);

          if (result.result().isPresent()) {
            return result;
          }

          String err = result.error().get().message();
          if (err.contains("No key") && err.contains(" in ")) {
            continue;
          }

          if (errors == null) {
            errors = result;
            continue;
          }

          errors = errors.apply2((p1, p2) -> p2, result);
        }

        if (errors == null) {
          return Results.error("No 'item', 'hardcoded', 'open-menu' or 'sell' value set :(");
        }

        return errors;
      }

      @Override
      public <T> DataResult<T> encode(Node input, DynamicOps<T> ops, T prefix) {
        return Results.error("No-op");
      }
    };
  }

  interface Node {

    MenuNode build(PageLoader loader, SellShopPage current);
  }

  record ItemNode(ItemStack item, List<String> commands) implements Node {

    static final Codec<ItemNode> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              SellShopCodecs.ITEM_CODEC.fieldOf("item")
                  .forGetter(o -> o.item),

              ExtraCodecs.listOrValue(Codec.STRING)
                  .optionalFieldOf("on-click", List.of())
                  .forGetter(o -> o.commands)
          )
          .apply(instance, ItemNode::new);
    });

    @Override
    public MenuNode build(PageLoader loader, SellShopPage current) {
      return MenuNode.builder()
          .setItem(user -> {
            var cloned = item.clone();
            var meta = cloned.getItemMeta();

            PlaceholderRenderer renderer = Placeholders.newRenderer().useDefaults();
            Placeholders.replaceItemPlaceholders(renderer, meta, user);

            cloned.setItemMeta(meta);
            return cloned;
          })

          .setRunnable((user, context, click) -> {
            if (commands.isEmpty()) {
              return;
            }

            Player player = user.getPlayer();

            for (String command : commands) {
              String replaced = Commands.replacePlaceholders(command, player);
              Commands.executeConsole(replaced);
            }

            click.shouldReloadMenu(true);
          })
          .build();
    }
  }

  record DirectNode(MenuNode node) implements Node {

    @Override
    public MenuNode build(PageLoader loader, SellShopPage current) {
      return node;
    }
  }

  record OpenMenuNode(String menuName) implements Node {

    @Override
    public MenuNode build(PageLoader loader, SellShopPage current) {
      Registry<SellShopPage> pages = loader.pages;

      return MenuNode.builder()
          .setItem((user, context) -> {
            return pages.get(menuName)
                .map(page -> page.createItem(user, context))
                .orElse(null);
          })
          .setRunnable((user, context, click) -> {
            Optional<SellShopPage> opt = pages.get(menuName);

            if (opt.isEmpty()) {
              LOGGER.warn("Couldn't find page named '{}'", menuName);
              return;
            }

            SellShopPage page = opt.get();

            Stack<SellShopPage> pageStack = context.get(SellShop.PAGE_STACK);

            if (pageStack == null) {
              pageStack = new Stack<>();
              context.set(SellShop.PAGE_STACK, pageStack);
            }

            pageStack.push(current);

            page.onClick(user, context, click);
          })
          .build();
    }
  }

  record SellableNode(ItemSellData data, boolean autoSellToggleAllowed) implements Node {

    @Override
    public MenuNode build(PageLoader loader, SellShopPage current) {
      return SellShopNodes.sellNode(data, autoSellToggleAllowed);
    }
  }

  static class LoadingPage {
    int size = UNSET_SIZE;

    String ownKey;

    boolean abstractPage = false;
    String extendsName;

    Component title;
    Component[] desc;

    Material headerItem;
    ItemStack border;
    Node[] nodes = new Node[Menus.MAX_INV_SIZE];

    Style nameStyle = Style.style(NamedTextColor.YELLOW);

    SellShopPage build(PageLoader loader) {
      SellShopPage page = new SellShopPage();
      page.size = size;
      page.title = title;
      page.border = border;
      page.headerItem = headerItem;
      page.nameStyle = nameStyle;

      if (desc == null) {
        page.desc = new Component[0];
      } else {
        page.desc = desc;
      }

      MenuNode[] builtNodes = new MenuNode[nodes.length];

      for (int i = 0; i < nodes.length; i++) {
        Node node = nodes[i];

        if (node == null) {
          continue;
        }

        builtNodes[i] = node.build(loader, page);
      }

      page.nodes = builtNodes;
      page.initialize();

      return page;
    }

    void extend(LoadingPage parent) {
      if (size == UNSET_SIZE) {
        size = parent.size;
      }

      if (headerItem == null) {
        headerItem = parent.headerItem;
      }

      if (desc == null || desc.length < 1) {
        desc = parent.desc;
      }

      if (ItemStacks.isEmpty(border)) {
        border = parent.border;
      }

      if (title == null) {
        title = parent.title;
      }

      for (int i = 0; i < parent.nodes.length; i++) {
        Node parentNode = parent.nodes[i];
        Node thisNode = nodes[i];

        if (parentNode == null) {
          continue;
        }

        if (thisNode == null) {
          nodes[i] = parentNode;
        }
      }
    }
  }
}
