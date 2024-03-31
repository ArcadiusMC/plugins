package net.arcadiusmc.holograms.commands;

import static net.arcadiusmc.text.Text.format;
import static net.arcadiusmc.text.Text.formatNumber;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.chat.MessageSuggestions;
import net.arcadiusmc.holograms.BoardImpl;
import net.arcadiusmc.holograms.Leaderboard.Order;
import net.arcadiusmc.holograms.HologramPlugin;
import net.arcadiusmc.holograms.LeaderboardSource;
import net.arcadiusmc.holograms.LeaderboardSources;
import net.arcadiusmc.holograms.ServiceImpl;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.text.page.Footer;
import net.arcadiusmc.text.page.Header;
import net.arcadiusmc.text.page.PageFormat;
import net.arcadiusmc.text.page.PagedIterator;
import net.arcadiusmc.utils.context.Context;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.forthecrown.grenadier.types.ParsedPosition;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.spongepowered.math.vector.Vector3f;

@CommandFile("leaderboards.gcn")
public class CommandLeaderboard {

  private final PageFormat<BoardImpl> pageFormat;

  private final HologramPlugin plugin;
  private final ServiceImpl service;

  public CommandLeaderboard(HologramPlugin plugin) {
    this.service = plugin.getService();
    this.plugin = plugin;

    pageFormat = PageFormat.create();

    pageFormat.setHeader(
        Header.<BoardImpl>create().title((it, writer, context) -> {
          writer.write(Messages.render("leaderboards.list.header"));
        })
    );

    pageFormat.setFooter(Footer.ofButton("/lb list %s %s"));

    pageFormat.setEntry((writer, entry, viewerIndex, context, it) -> {
      writer.write(entry.displayName());
    });
  }

  @VariableInitializer
  void initVars(Map<String, Object> vars) {
    vars.put("lb", new LeaderboardArgument(service));
    vars.put("source", new SourceArgument(service));
    vars.put("color", Arguments.COLOR);
    vars.put("filter", ArgumentTypes.intRange());
    vars.put("alignment", ArgumentTypes.enumType(TextAlignment.class));
    vars.put("billboard", ArgumentTypes.enumType(Billboard.class));
    vars.put("order", ArgumentTypes.enumType(Order.class));
  }

  void reloadConfig(CommandSource source) {
    plugin.reloadConfig();
    source.sendSuccess(Messages.renderText("leaderboards.reloaded.config", source));
  }

  void reloadBoards(CommandSource source) {
    service.load();
    source.sendSuccess(Messages.renderText("leaderboards.reloaded.plugin", source));
  }

  void saveBoards(CommandSource source) {
    service.save();
    source.sendSuccess(Messages.renderText("leaderboards.saved", source));
  }

  void listBoards(
      CommandSource source,
      @Argument(value = "page", optional = true) Integer pageArg,
      @Argument(value = "pageSize", optional = true) Integer pageSizeArg
  ) throws CommandSyntaxException {
    List<BoardImpl> boards = new ArrayList<>(service.getBoards().values());
    boards.sort(Comparator.comparing(BoardImpl::getName));

    int page = pageArg == null ? 0 : (pageArg - 1);
    int pageSize = pageSizeArg == null ? 10 : pageSizeArg;

    Commands.ensurePageValid(page, pageSize, boards.size());

    PagedIterator<BoardImpl> it = PagedIterator.of(boards, page, pageSize);
    TextWriter writer = TextWriters.newWriter();
    writer.viewer(source);
    pageFormat.write(it, writer, Context.EMPTY);

    Component pageDisplay = writer.asComponent();

    source.sendMessage(pageDisplay);
  }

  void showInfo(CommandSource source, @Argument("board") BoardImpl board) {
    source.sendMessage(board.infoText());
  }

  void updateBoard(CommandSource source, @Argument("board") BoardImpl board)
      throws CommandSyntaxException
  {
    if (!board.isSpawned()) {
      throw Messages.render("leaderboards.errors.notSpawned")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.update();

    source.sendSuccess(
        Messages.render("leaderboards.updated")
            .addValue("board", board.displayName())
            .create(source)
    );
  }

  void createBoard(CommandSource source, @Argument("name") String name)
      throws CommandSyntaxException
  {
    var opt = service.getLeaderboard(name);
    if (opt.isPresent()) {
      throw Messages.render("leaderboards.errors.alreadyExists")
          .addValue("name", name)
          .exception(source);
    }

    BoardImpl board = new BoardImpl(name);
    board.setSource(LeaderboardSources.DUMMY_HOLDER);

    service.addLeaderboard(board);

    source.sendSuccess(
        Messages.render("leaderboards.created")
            .addValue("board", board.displayName())
            .create(source)
    );
  }

  void updateAll(CommandSource source) {
    int updated = 0;

    for (BoardImpl board : service.getBoards().values()) {
      if (!board.isSpawned()) {
        continue;
      }

      updated++;
      board.update();
    }

    source.sendSuccess(
        Messages.render("leaderboards.updateAll")
            .addValue("count", updated)
            .create(source)
    );
  }

  void killBoard(CommandSource source, @Argument("board") BoardImpl board)
      throws CommandSyntaxException
  {
    if (!board.isSpawned()) {
      throw Messages.render("leaderboards.errors.alreadyInactive")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.kill();

    source.sendSuccess(
        Messages.render("leaderboards.killed")
            .addValue("board", board)
            .create(source)
    );
  }

  void removeBoard(CommandSource source, @Argument("board") BoardImpl board)
      throws CommandSyntaxException
  {
    if (board.isSpawned()) {
      board.kill();
    }

    Component name = board.displayName();

    service.removeLeaderboard(board.getName());

    source.sendSuccess(
        Messages.render("leaderboards.removed")
            .addValue("board", name)
            .create(source)
    );
  }

  void spawnBoard(CommandSource source, @Argument("board") BoardImpl board)
      throws CommandSyntaxException
  {
    if (board.isSpawned()) {
      throw Messages.render("leaderboards.errors.alreadySpawned")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.spawn();

    source.sendSuccess(
        Messages.render("leaderboards.spawned")
            .addValue("board", board.displayName())
            .create(source)
    );
  }

  void setLocation(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "pos", optional = true) Location locationArg
  ) {
    Location location;

    if (locationArg == null) {
      location = source.getLocation();
    } else {
      location = locationArg;
    }

    board.setLocation(location);

    source.sendSuccess(
        Messages.render("leaderboards.location")
            .addValue("board", board.displayName())
            .addValue("location", location)
            .create(source)
    );
  }

  void copyBoard(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("source") BoardImpl copySource
  ) throws CommandSyntaxException {
    if (Objects.equals(board, copySource)) {
      throw Messages.render("leaderboards.errors.copySelf")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.copyFrom(copySource);
    board.update();

    source.sendSuccess(
        Messages.render("leaderboards.copied")
            .addValue("source", copySource.displayName())
            .addValue("target", board.displayName())
            .create(source)
    );
  }

  Component getSetMessage(Audience viewer, BoardImpl board, String suffix, Object value) {
    MessageRender render;
    String regular = "leaderboards." + suffix;

    if (value == null) {
      render = Messages.render(regular, "removed");
    } else {
      String set = regular + ".set";

      if (Messages.MESSAGE_LIST.hasMessage(set)) {
        render = Messages.render(set);
      } else {
        render = Messages.render(regular);
      }
    }

    return render
        .addValue("board", board.displayName())
        .addValue("value", value)
        .create(viewer);
  }

  void setTextField(
      BoardImpl board,
      CommandSource source,
      String name,
      BiConsumer<BoardImpl, PlayerMessage> setter,
      String text
  ) {
    if (text == null) {
      setter.accept(board, null);
    } else {
      setter.accept(board, BoardImpl.makeTextFieldMessage(text));
    }

    source.sendSuccess(getSetMessage(source, board, name, text));

    board.update();
  }

  CompletableFuture<Suggestions> suggestTextField(
      PlayerMessage message,
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    if (message == null) {
      return MessageSuggestions.get(context, builder, true);
    }

    return MessageSuggestions.get(context, builder, true, (builder1, source) -> {
      builder1.suggest(message.getMessage());
    });
  }

  CompletableFuture<Suggestions> suggestHeader(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder,
      @Argument("board") BoardImpl board
  ) {
    return suggestTextField(board.getHeader(), context, builder);
  }

  CompletableFuture<Suggestions> suggestFormat(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder,
      @Argument("board") BoardImpl board
  ) {
    return suggestTextField(board.getFormat(), context, builder);
  }

  CompletableFuture<Suggestions> suggestFooter(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder,
      @Argument("board") BoardImpl board
  ) {
    return suggestTextField(board.getFooter(), context, builder);
  }

  void setFooter(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "text", optional = true) String text
  ) {
    setTextField(board, source, "footer", BoardImpl::setFooter, text);
  }

  void setHeader(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "text", optional = true) String text
  ) {
    setTextField(board, source, "header", BoardImpl::setHeader, text);
  }

  void setFormat(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "text", optional = true) String text
  ) {
    setTextField(board, source, "format", BoardImpl::setFormat, text);
  }

  void setYouFormat(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "text", optional = true) String text
  ) {
    setTextField(board, source, "youFormat", BoardImpl::setYouFormat, text);
  }

  void setEmptyFormat(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "text", optional = true) String text
  ) {
    setTextField(board, source, "emptyFormat", BoardImpl::setEmptyFormat, text);
  }

  void setIncludeYou(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") boolean include
  ) {
    board.setIncludeYou(include);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "includeYou", include));
  }

  void setOrder(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("order") Order order
  ) {
    board.setOrder(order);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "order", order));
  }


  void setSource(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("source") Holder<LeaderboardSource> holder
  ) {
    board.setSource(holder);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "source", holder.getKey()));
  }

  void setMaxSize(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("size") int maxSize
  ) {
    board.setMaxEntries(maxSize);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "maxSize", maxSize));
  }

  void setFillEmpty(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") boolean value
  ) {
    board.setFillMissingSlots(value);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "fillEmpty", value));
  }

  void setFilter(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "filter", optional = true) IntRange filter
  ) {
    board.setFilter(filter);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "filter", filter));
  }

  void setYaw(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") float value
  ) {
    board.getDisplayMeta().setYaw(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "yaw", formatNumber(value)));
  }

  void setPitch(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") float value
  ) {
    board.getDisplayMeta().setPitch(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "pitch", formatNumber(value)));
  }

  Vector3f toVec(ParsedPosition position) throws CommandSyntaxException {
    if (position.getXCoordinate().relative()
        || position.getYCoordinate().relative()
        || position.getZCoordinate().relative()
    ) {
      throw Exceptions.create("No relative coordinates ('~' or '^') not allowed here");
    }

    return new Vector3f(
        position.getXCoordinate().value(),
        position.getYCoordinate().value(),
        position.getZCoordinate().value()
    );
  }

  void setScale(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "value", optional = true) ParsedPosition position
  ) throws CommandSyntaxException {
    if (position == null) {
      board.getDisplayMeta().setScale(Vector3f.ONE);
      source.sendSuccess(getSetMessage(source, board, "scale", null));
    } else {
      Vector3f vector = toVec(position);
      board.getDisplayMeta().setScale(vector);
      source.sendSuccess(getSetMessage(source, board, "scale", format("{0, vector}", vector)));
    }

    board.update();
  }

  void setTranslation(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "value", optional = true) ParsedPosition position
  ) throws CommandSyntaxException {
    if (position == null) {
      board.getDisplayMeta().setTranslation(Vector3f.ONE);
      source.sendSuccess(getSetMessage(source, board, "offset", null));
    } else {
      Vector3f vector = toVec(position);
      board.getDisplayMeta().setTranslation(vector);
      source.sendSuccess(getSetMessage(source, board, "offset", format("{0, vector}", vector)));
    }

    board.update();
  }

  void setBillboard(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") Billboard billboard
  ) throws CommandSyntaxException {
    board.getDisplayMeta().setBillboard(billboard);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "billboard", billboard));
  }

  void setAlign(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") TextAlignment alignment
  ) throws CommandSyntaxException {
    board.getDisplayMeta().setAlign(alignment);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "align", alignment));
  }

  void setBackColor(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "value", optional = true) Color color
  ) {
    board.getDisplayMeta().setBackgroundColor(color);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "backColor", color));
  }

  void setBrightness(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "skylight", optional = true) Integer skylight,
      @Argument(value = "blocklight", optional = true) Integer blocklight
  ) {
    if (skylight == null || blocklight == null) {
      board.getDisplayMeta().setBrightness(null);
      source.sendSuccess(getSetMessage(source, board, "brightness", null));
    } else {
      Brightness brightness = new Brightness(blocklight, skylight);
      board.getDisplayMeta().setBrightness(brightness);
      source.sendSuccess(getSetMessage(source, board, "brightness", brightness));
    }

    board.update();
  }

  void setShadowed(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") boolean value
  ) {
    board.getDisplayMeta().setShadowed(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "shadowed", value));
  }

  void setSeeThrough(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument("value") boolean value
  ) {
    board.getDisplayMeta().setSeeThrough(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "seeThrough", value));
  }

  void setLineWidth(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value = "value", optional = true) Integer lineWidth
  ) {
    if (lineWidth == null) {
      board.getDisplayMeta().setLineWidth(-1);
    } else {
      board.getDisplayMeta().setLineWidth(lineWidth);
    }

    board.update();
    source.sendSuccess(getSetMessage(source, board, "lineWidth", lineWidth));
  }

  void setOpacity(
      CommandSource source,
      @Argument("board") BoardImpl board,
      @Argument(value="value", optional = true) Integer opacity
  ) {
    if (opacity != null) {
      board.getDisplayMeta().setOpacity(opacity.byteValue());
    } else {
      board.getDisplayMeta().setOpacity((byte) -1);
    }

    board.update();
    source.sendSuccess(getSetMessage(source, board, "opacity", opacity));
  }
}
