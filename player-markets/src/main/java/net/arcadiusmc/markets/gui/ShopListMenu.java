package net.arcadiusmc.markets.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.McConstants;
import net.arcadiusmc.markets.ClaimHighlighter;
import net.arcadiusmc.markets.Entrance;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.gui.ShopLists.MenuConfig;
import net.arcadiusmc.markets.gui.ShopLists.MenuSettings;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.page.ListPage;
import net.arcadiusmc.text.BufferedTextWriter;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.inventory.SkullItemBuilder;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Vectors;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerTextures;
import org.spongepowered.math.vector.Vector3d;

public class ShopListMenu extends ListPage<Market> {

  // 81925748-86cc-4137-bac0-dea70461560d
  static final PlayerProfile UNOWNED_HEAD
      = createProfile("http://textures.minecraft.net/texture/61e974a2608bd6ee57f33485645dd922d16b4a39744ebab4753f4deb4ef782");

  static final PlayerProfile OWNED_HEAD
      = createProfile("http://textures.minecraft.net/texture/3e623211bda5dd5ab9e76030f86b1c4730b988726eef6a3ab28aa1c1f7d850");

  static final PlayerProfile HEADER
      = createProfile("http://textures.minecraft.net/texture/badc048a7ce78f7dad72a07da27d85c0916881e5522eeed1e3daf217a38c1a");

  static final String ALLOW_ALL = "*";

  private final MenuSettings settings;
  private final MenuConfig config;

  ShopListMenu(ContextOption<Integer> page, MenuSettings settings, MenuConfig config) {
    super(null, page);

    this.settings = settings;
    this.config = config;

    initMenu(
        Menus.builder(config.size(), config.title()),
        false
    );
  }

  @Override
  protected List<Market> getList(User user, Context context) {
    MarketsManager manager = Markets.getManager();
    List<Market> markets = new ArrayList<>(manager.getMarkets());

    markets.removeIf(market -> {
      for (String group : config.groups()) {
        if (Objects.equals(group, market.getGroupName())) {
          return false;
        }

        if (group.equalsIgnoreCase(ALLOW_ALL)) {
          return false;
        }
      }

      return true;
    });

    return markets;
  }

  @Override
  protected ItemStack getItem(User user, Market entry, Context context) {
    SkullItemBuilder builder = ItemStacks.headBuilder();
    boolean hasOwner = entry.getOwnerId() != null;

    if (!hasOwner) {
      builder.setProfile(UNOWNED_HEAD);
    } else {
      builder.setProfile(OWNED_HEAD);
    }

    BufferedTextWriter writer = TextWriters.buffered();
    writer.viewer(user);
    writeLore(writer, entry);
    builder.addLore(writer.getBuffer());

    Component name;
    PlaceholderRenderer renderer = Placeholders.newRenderer();

    if (hasOwner) {
      name = settings.takenName();
      renderer.add("owner", Users.get(entry.getOwnerId()).displayName(writer.viewer()));
    } else {
      name = settings.availableName();
    }

    name = renderer.render(name);
    builder.setName(name);

    return builder.build();
  }

  private void writeLore(TextWriter writer, Market market) {
    Component firstLine = Placeholders.render(settings.firstLine(), writer.viewer());

    if (!Text.isEmpty(firstLine)) {
      writer.line(firstLine);
      writer.newLine();
      writer.newLine();
    }

    if (market.getOwnerId() == null) {
      int price = market.getPriceFor(writer.viewer());

      writer.line(
          Placeholders.newRenderer()
              .add("price", UnitFormat.currency(price))
              .render(settings.priceLine())
      );
      return;
    }

    PlayerMessage desc = market.getDescription();
    if (desc != null) {
      writer.line(desc);
    }

    Set<UUID> members = market.getMembers();
    if (members.isEmpty()) {
      return;
    }

    writer.line("Members:", NamedTextColor.GOLD);

    for (UUID member : members) {
      User user = Users.get(member);
      writer.line(user.displayName(writer.viewer()));
    }
  }

  @Override
  protected void onClick(User user, Market entry, Context context, ClickContext click)
      throws CommandSyntaxException
  {
    teleportToEntrance(user, entry);
    click.shouldClose(true);

    MarketsPlugin plugin = entry.getManager().getPlugin();
    ClaimHighlighter highlighter = plugin.getHighlighter();

    highlighter.show(user, entry);
  }

  private void teleportToEntrance(User user, Market market) {
    List<Entrance> entrances = market.getEntrances();

    if (entrances.isEmpty()) {
      return;
    }

    Entrance first = entrances.get(0);
    Vector3d pos = first.entityPosition();
    Direction dir = Direction.fromBukkit(first.direction());
    Direction opposite = dir.opposite();

    Vector3d tp = pos.add(dir.getMod().toDouble().mul(2));
    World world = market.getWorld();

    Location location = new Location(world, tp.x(), tp.y(), tp.z());
    location.setDirection(Vectors.toVec(opposite.getMod()));

    findHighestAir(location);

    user.getPlayer().teleport(location);
  }

  void findHighestAir(Location location) {
    double originalY = location.getBlockY();

    while (true) {
      if (location.getY() < McConstants.MIN_Y) {
        location.setY(originalY);
        return;
      }

      location.subtract(0, 1, 0);
      Block block = location.getBlock();

      if (block.isCollidable()) {
        continue;
      }

      location.add(0, 1, 0);
      return;
    }
  }

  @Override
  protected MenuNode createHeader() {
    return MenuNode.builder()
        .setItem(createHeaderItem())
        .build();
  }

  private ItemStack createHeaderItem() {
    SkullItemBuilder builder = ItemStacks.headBuilder();
    builder.setProfile(HEADER);

    List<Component> desc = config.description();
    Component name = Placeholders.render(config.headerName());

    if (settings.alignItemNames()) {
      int maxLen = 0;

      for (Component component : desc) {
        int lineLen = TextInfo.length(component);
        maxLen = Math.max(lineLen, maxLen);
      }

      int nameLen = TextInfo.length(name);
      int dif = maxLen - nameLen;
      int spaces = (dif / 2) / TextInfo.getCharPxWidth(' ');

      if (spaces > 0) {
        builder.setName(
            Component.textOfChildren(
                Component.text(" ".repeat(spaces)),
                name
            )
        );
      } else {
        builder.setName(name);
      }
    } else {
      builder.setName(name);
    }

    desc.stream()
        .map(Placeholders::render)
        .forEach(builder::addLore);

    return builder.build();
  }

  static PlayerProfile createProfile(String textureUrl) {
    PlayerProfile profile = Bukkit.getServer().createProfile(Identity.nil().uuid());

    try {
      PlayerTextures textures = profile.getTextures();
      textures.setSkin(new URL(textureUrl));
      profile.setTextures(textures);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    if (!profile.hasTextures()) {
      CompletableFuture.runAsync(() -> {
        profile.complete(true);
      });
    }

    return profile;
  }
}
