package net.arcadiusmc.holograms.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.arcadiusmc.command.arguments.chat.MessageSuggestions;
import net.arcadiusmc.holograms.BoardImpl;
import net.arcadiusmc.holograms.Hologram;
import net.arcadiusmc.holograms.HologramPlugin;
import net.arcadiusmc.holograms.Leaderboard.Order;
import net.arcadiusmc.holograms.LeaderboardSource;
import net.arcadiusmc.holograms.LeaderboardSources;
import net.arcadiusmc.holograms.ServiceImpl;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.text.Component;

@CommandFile("leaderboards.gcn")
public class CommandLeaderboard extends DisplayCommand {

  private final HologramPlugin plugin;

  public CommandLeaderboard(HologramPlugin plugin) {
    super(plugin.getService(), "leaderboards", "/lb list %s %s");
    this.plugin = plugin;
  }

  @Override
  Collection<? extends Hologram> getList(ServiceImpl service) {
    return service.getBoards().values();
  }

  @VariableInitializer
  void initVars(Map<String, Object> vars) {
    super.initVariables(vars);

    vars.put("lb", new LeaderboardArgument(service));
    vars.put("display", new LeaderboardArgument(service));
    vars.put("source", new SourceArgument(service));
    vars.put("filter", ArgumentTypes.intRange());
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
}
