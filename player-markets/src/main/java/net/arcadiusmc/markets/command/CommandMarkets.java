package net.arcadiusmc.markets.command;

import com.google.common.base.Strings;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.McConstants;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.markets.Entrance;
import net.arcadiusmc.markets.Eviction;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.text.page.Footer;
import net.arcadiusmc.text.page.Header;
import net.arcadiusmc.text.page.PageFormat;
import net.arcadiusmc.text.page.PagedIterator;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

@CommandFile("commands/markets.gcn")
public class CommandMarkets {

  private final MarketsPlugin plugin;
  private final PageFormat<Market> marketPageFormat;

  public CommandMarkets(MarketsPlugin plugin) {
    this.plugin = plugin;

    marketPageFormat = PageFormat.create();
    marketPageFormat.setHeader(
        Header.<Market>create().title((it, writer, context) -> {
          writer.write(Messages.render("markets.cmd.list.header"));
        })
    );

    marketPageFormat.setEntry((writer, entry, viewerIndex, context, it) -> {
      writer.write(entry.displayName());
    });

    marketPageFormat.setFooter(Footer.ofButton("/markets list %s %s"));
  }

  @VariableInitializer
  void initVars(Map<String, Object> vars) {
    vars.put("market", MarketCommands.argument);
  }

  void savePlugin(CommandSource source) {
    plugin.save();
    source.sendMessage(Messages.renderText("markets.cmd.saved", source));
  }

  void reloadConfig(CommandSource source) {
    plugin.reloadConfig();
    source.sendSuccess(Messages.renderText("markets.cmd.reloaded.config"));
  }

  void reloadPlugin(CommandSource source) {
    plugin.reload();
    source.sendSuccess(Messages.renderText("markets.cmd.reloaded.plugin"));
  }

  void listMarkets(
      CommandSource source,
      @Argument(value = "page", optional = true) Integer page,
      @Argument(value = "page-size", optional = true) Integer pageSize
  ) throws CommandSyntaxException {
    if (page == null) {
      page = 1;
    }
    if (pageSize == null) {
      pageSize = 5;
    }

    List<Market> markets = new ArrayList<>(plugin.getManager().getMarkets());

    if (markets.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    PagedIterator<Market> iterator = PagedIterator.of(markets, page - 1, pageSize);
    Component list = marketPageFormat.format(iterator);

    source.sendMessage(list);
  }

  void createMarket(CommandSource source, @Argument("region") String regionName)
      throws CommandSyntaxException
  {
    World world = source.getWorld();

    RegionContainer container = WorldGuard.getInstance()
        .getPlatform()
        .getRegionContainer();

    RegionManager manager = container.get(BukkitAdapter.adapt(world));
    if (manager == null) {
      throw Messages.render("markets.errors.noRegionsInWorld").exception(source);
    }

    ProtectedRegion region = manager.getRegion(regionName);
    if (region == null) {
      throw Messages.render("markets.errors.unknownRegion")
          .addValue("region", regionName)
          .exception(source);
    }

    Market market = plugin.getManager().getMarket(regionName);
    if (market != null) {
      throw Messages.render("markets.errors.alreadyExists")
          .addValue("region", regionName)
          .exception(source);
    }

    market = new Market(world.getName(), regionName);
    plugin.getManager().addMarket(market);

    source.sendSuccess(
        Messages.render("markets.cmd.created")
            .addValue("region", regionName)
            .create(source)
    );
  }

  CompletableFuture<Suggestions> suggestRegionNames(
      SuggestionsBuilder builder,
      CommandSource source
  ) {
    World world = source.getWorld();

    RegionContainer container = WorldGuard.getInstance()
        .getPlatform()
        .getRegionContainer();

    RegionManager manager = container.get(BukkitAdapter.adapt(world));
    if (manager == null) {
      return Suggestions.empty();
    }

    return Completions.suggest(builder, manager.getRegions().keySet());
  }

  void removeMarket(CommandSource source, @Argument("market") Market market) {
    Component displayName = market.displayName();

    plugin.getManager().deleteMarket(market);

    source.sendSuccess(
        Messages.render("markets.cmd.removed")
            .addValue("market", displayName)
            .create(source)
    );
  }

  void claim(CommandSource source, @Argument("market") Market market, @Argument("user") User user)
      throws CommandSyntaxException
  {
    if (market.getOwnerId() != null) {
      throw Messages.render("markets.errors.alreadyClaimed")
          .addValue("market", market.displayName())
          .exception(source);
    }

    Market owned = Markets.getOwned(user);
    if (owned != null) {
      throw Messages.render("markets.errors.alreadyOwnsShop")
          .addValue("player", user)
          .exception(source);
    }

    market.claim(user);

    source.sendSuccess(
        Messages.render("markets.cmd.claimed")
            .addValue("player", user)
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void unclaim(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    if (market.getOwnerId() == null) {
      throw Messages.render("markets.errors.notClaimed")
          .addValue("market", market.displayName())
          .exception(source);
    }

    market.unclaim();

    source.sendSuccess(
        Messages.render("markets.cmd.unclaimed")
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void reset(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    plugin.getResets().reset(market);

    source.sendSuccess(
        Messages.render("markets.cmd.reset")
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void merge(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("other") Market other
  ) throws CommandSyntaxException {
    if (market.getMerged() != null) {
      throw Messages.render("markets.errors.alreadyMerged")
          .addValue("market", market.displayName())
          .addValue("other", market.getMerged().displayName())
          .exception(source);
    }

    market.merge(other);

    source.sendSuccess(
        Messages.render("markets.cmd.merged")
            .addValue("market", market.displayName())
            .addValue("other", other.displayName())
            .create(source)
    );
  }

  void unmerge(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    Market merged = market.getMerged();

    if (merged == null) {
      throw Messages.render("markets.errors.notMerged.with")
          .addValue("market", market.displayName())
          .exception(source);
    }

    market.unmerge();

    source.sendSuccess(
        Messages.render("markets.cmd.unmerged")
            .addValue("market", market.displayName())
            .addValue("other", merged.displayName())
            .create(source)
    );
  }

  CompletableFuture<Suggestions> suggestReasons(SuggestionsBuilder builder) {
    return Completions.suggest(
        builder,
        "\"Too few shops\"",
        "\"Out of stock\"",
        "\"Owner inactive\""
    );
  }

  void startEviction(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("reason") String reason,
      @Argument(value = "delay", optional = true) Duration delay
  ) throws CommandSyntaxException {
    MarketsManager manager = plugin.getManager();

    if (manager.isMarkedForEviction(market)) {
      throw Messages.render("markets.errors.eviction.alreadyMarked")
          .addValue("market", market.displayName())
          .exception(source);
    }
    if (market.getOwnerId() == null) {
      throw Messages.render("markets.errors.notClaimed")
          .addValue("market", market.displayName())
          .exception(source);
    }

    if (delay == null) {
      delay = plugin.getAutoEvictions().getConfig().evictionDelay();
    }

    manager.beginEviction(market, source.textName(), Text.renderString(reason), delay);

    Instant evictionDate = Instant.now().plus(delay);

    source.sendSuccess(
        Messages.render("markets.cmd.eviction.begin")
            .addValue("market", market.displayName())
            .addValue("reason", Text.renderString(reason))
            .addValue("delay", delay)
            .addValue("date", evictionDate)
            .create(source)
    );
  }

  void stopEviction(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    MarketsManager manager = plugin.getManager();

    if (!manager.isMarkedForEviction(market)) {
      throw Messages.render("markets.errors.eviction.notMarked")
          .addValue("market", market.displayName())
          .exception(source);
    }

    manager.stopEviction(market);

    source.sendSuccess(
        Messages.render("markets.cmd.eviction.stopped")
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void showEvictionInfo(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    MarketsManager manager = plugin.getManager();

    if (!manager.isMarkedForEviction(market)) {
      throw Messages.render("markets.errors.eviction.notMarked")
          .addValue("market", market.displayName())
          .exception(source);
    }

    Eviction eviction = manager.getEviction(market);

    source.sendMessage(
        Messages.render("markets.cmd.eviction.info")
            .addValue("market", market.displayName())
            .addValue("source", eviction.getSource())
            .addValue("reason", eviction.getReason())
            .addValue("date", eviction.getDate())
            .addValue("start", eviction.getStart())
            .create(source)
    );
  }

  void setPrice(
      CommandSource source,
      @Argument("market") Market market,
      @Argument(value = "value", optional = true) Integer value
  ) {
    String messageKeySuffix;

    if (value == null) {
      value = -1;
      messageKeySuffix = "unset";
    } else {
      messageKeySuffix = "set";
    }

    market.setPrice(value);

    source.sendSuccess(
        Messages.render("markets.cmd.price", messageKeySuffix)
            .addValue("market", market.displayName())
            .addValue("price", UnitFormat.currency(value))
            .create(source)
    );
  }

  void setRent(
      CommandSource source,
      @Argument("market") Market market,
      @Argument(value = "value", optional = true) Integer value
  ) {
    String messageKeySuffix;

    if (value == null) {
      value = -1;
      messageKeySuffix = "unset";
    } else {
      messageKeySuffix = "set";
    }

    market.setRent(value);

    source.sendSuccess(
        Messages.render("markets.cmd.rent", messageKeySuffix)
            .addValue("market", market.displayName())
            .addValue("price", UnitFormat.currency(value))
            .create(source)
    );
  }

  void listConnections(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    MarketsManager manager = plugin.getManager();

    List<Component> connected = market.getConnectedNames()
        .stream()
        .map(manager::getMarket)
        .filter(Objects::nonNull)
        .map(Market::displayName)
        .map(component -> {
          return Messages.render("markets.cmd.connections.list.format")
              .addValue("connected", component)
              .create(source);
        })
        .toList();

    if (connected.isEmpty()) {
      throw Messages.render("markets.errors.noConnections")
          .addValue("market", market.displayName())
          .exception(source);
    }

    TextJoiner joiner = TextJoiner.newJoiner().add(connected);

    source.sendMessage(
        Messages.render("markets.cmd.connections.list.header")
            .addValue("market", market.displayName())
            .addValue("list", joiner.asComponent())
            .create(source)
    );
  }

  void connect(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("other") Market other
  ) throws CommandSyntaxException {
    if (market.isConnected(other)) {
      throw Messages.render("markets.errors.alreadyConnected")
          .addValue("market", market.displayName())
          .addValue("other", other.displayName())
          .exception(source);
    }

    market.connect(other);

    source.sendSuccess(
        Messages.render("markets.cmd.connections.created")
            .addValue("market", market.displayName())
            .addValue("other", other.displayName())
            .create(source)
    );
  }

  void disconnect(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("other") Market other
  ) throws CommandSyntaxException {
    if (!market.isConnected(other)) {
      throw Messages.render("markets.errors.notConnected")
          .addValue("market", market.displayName())
          .addValue("other", other.displayName())
          .exception(source);
    }

    market.disconnect(other);

    source.sendSuccess(
        Messages.render("markets.cmd.connections.removed")
            .addValue("market", market.displayName())
            .addValue("other", other.displayName())
            .create(source)
    );
  }

  void clearConnections(CommandSource source, @Argument("market") Market market) {
    market.clearConnected();

    source.sendSuccess(
        Messages.render("markets.cmd.connections.cleared")
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void listEntrances(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    List<Entrance> entrances = market.getEntrances();

    if (entrances.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    TextJoiner joiner = TextJoiner.newJoiner()
        .add(
            entrances.stream()
                .map(Entrance::display)
                .map(component -> {
                  return Messages.render("markets.cmd.entrances.list.format")
                      .addValue("entrance", component)
                      .create(source);
                })
        );

    source.sendMessage(
        Messages.render("markets.cmd.entrances.list.header")
            .addValue("market", market.displayName())
            .addValue("list", joiner.asComponent())
            .create(source)
    );
  }

  void addEntrance(CommandSource source, @Argument("market") Market market)
      throws CommandSyntaxException
  {
    Player player = source.asPlayer();

    Location playerLoc = player.getLocation().add(0, 1, 0).toCenterLocation();
    playerLoc.setY(playerLoc.getBlockY());

    Vector3d noticePos = Vectors.doubleFrom(playerLoc);
    Vector3i doorSignPos = figureSignLoc(player);

    Entrance entrance = new Entrance(doorSignPos, noticePos, player.getFacing());
    market.getEntrances().add(entrance);

    World world = market.getWorld();
    if (world != null) {
      if (market.getOwnerId() == null) {
        entrance.onUnclaim(world, market);
      } else {
        User user = Users.get(market.getOwnerId());
        entrance.onClaim(user, world);
      }
    }

    source.sendSuccess(Messages.renderText("markets.cmd.entrances.added", source));
  }

  Vector3i figureSignLoc(Player player) {
    World w = player.getWorld();
    Vector3i pos = Vectors.intFrom(player.getLocation());

    BlockFace face = player.getFacing();

    while (Vectors.getBlock(pos, w).isEmpty() && pos.y() < McConstants.MAX_Y) {
      pos = pos.add(0, 1, 0);
    }

    int iteration = 0;
    while (Vectors.getBlock(pos, w).isSolid() && iteration < 10) {
      pos = pos.add(face.getModX(), face.getModY(), face.getModZ());
      iteration++;
    }

    return pos;
  }

  void removeEntrance(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("index") int index
  ) throws CommandSyntaxException {
    List<Entrance> entrances = market.getEntrances();
    Commands.ensureIndexValid(index, entrances.size());

    Entrance entrance = entrances.remove(index - 1);
    World world = market.getWorld();

    if (world != null) {
      entrance.remove(world);
    }

    source.sendSuccess(
        Messages.render("markets.cmd.entrances.removed")
            .addValue("index", index)
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void getGroup(CommandSource source, @Argument("market") Market market) {
    String group;

    if (Strings.isNullOrEmpty(market.getGroupName())) {
      group = "not-set";
    } else {
      group = market.getGroupName();
    }

    source.sendMessage(
        Messages.render("markets.cmd.groups.get")
            .addValue("group", group)
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  void setGroup(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("group") String str
  ) throws CommandSyntaxException {
    if (str.equalsIgnoreCase("not-set") || str.equalsIgnoreCase("none")) {
      unsetGroup(source, market);
      return;
    }

    market.setGroupName(str);

    source.sendSuccess(
        Messages.render("markets.cmd.group.set")
            .addValue("market", market.displayName())
            .addValue("group", str)
            .create(source)
    );
  }

  void unsetGroup(
      CommandSource source,
      @Argument("market") Market market
  ) throws CommandSyntaxException {
    if (Strings.isNullOrEmpty(market.getGroupName())) {
      throw Messages.render("markets.errors.alreadyUngrouped")
          .addValue("market", market.displayName())
          .exception(source);
    }

    market.setGroupName(null);

    source.sendSuccess(
        Messages.render("markets.cmd.group.unset")
            .addValue("market", market.displayName())
            .create(source)
    );
  }

  CompletableFuture<Suggestions> suggestGroups(SuggestionsBuilder builder) {
    return Completions.suggest(builder,
        plugin.getManager()
            .getMarkets()
            .stream()
            .map(Market::getGroupName)
            .filter(Objects::nonNull)
    );
  }

  void getRenderSize(CommandSource source, @Argument("market") Market market) {
    int off = market.getBoundRenderOffset();

    source.sendMessage(
        Messages.render("markets.cmd.sizeOffset.get")
            .addValue("market", market.displayName())
            .addValue("value", off)
            .create(source)
    );
  }

  void setRenderSize(
      CommandSource source,
      @Argument("market") Market market,
      @Argument("value") int off
  ) {
    market.setBoundRenderOffset(off);

    source.sendSuccess(
        Messages.render("markets.cmd.sizeOffset.get")
            .addValue("market", market.displayName())
            .addValue("value", off)
            .create(source)
    );
  }
}
