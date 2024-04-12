package net.arcadiusmc.holograms.commands;

import static net.arcadiusmc.text.Text.format;
import static net.arcadiusmc.text.Text.formatNumber;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.holograms.Hologram;
import net.arcadiusmc.holograms.ServiceImpl;
import net.arcadiusmc.text.Messages;
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
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ParsedPosition;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.spongepowered.math.vector.Vector3f;

public abstract class DisplayCommand {
  
  static final String DISPLAY_ARG = "display";

  final ServiceImpl service;

  final String messagePrefix;
  final PageFormat<Hologram> pageFormat;
  
  public DisplayCommand(
      ServiceImpl service,
      String messagePrefix,
      String listCommand
  ) {
    this.service = service;
    this.messagePrefix = messagePrefix;

    pageFormat = PageFormat.create();

    pageFormat.setHeader(
        Header.<Hologram>create().title((it, writer, context) -> {
          writer.write(Messages.render(messagePrefix, "list.header"));
        })
    );

    pageFormat.setFooter(Footer.ofButton(listCommand));

    pageFormat.setEntry((writer, entry, viewerIndex, context, it) -> {
      writer.write(entry.displayName());
    });
  }

  @VariableInitializer
  void initVariables(Map<String, Object> vars) {
    vars.put("color", Arguments.COLOR);
    vars.put("alignment", ArgumentTypes.enumType(TextAlignment.class));
    vars.put("billboard", ArgumentTypes.enumType(Billboard.class));
  }

  abstract Collection<? extends Hologram> getList(ServiceImpl service);
  
  void listDisplays(
      CommandSource source,
      @Argument(value = "page", optional = true) Integer pageArg,
      @Argument(value = "pageSize", optional = true) Integer pageSizeArg
  ) throws CommandSyntaxException {
    List<Hologram> boards = new ArrayList<>(getList(service));
    boards.sort(Comparator.comparing(Hologram::getName));

    int page = pageArg == null ? 0 : (pageArg - 1);
    int pageSize = pageSizeArg == null ? 10 : pageSizeArg;

    Commands.ensurePageValid(page, pageSize, boards.size());

    PagedIterator<Hologram> it = PagedIterator.of(boards, page, pageSize);
    TextWriter writer = TextWriters.newWriter();
    writer.viewer(source);
    pageFormat.write(it, writer, Context.EMPTY);

    Component pageDisplay = writer.asComponent();

    source.sendMessage(pageDisplay);
  }

  void showInfo(CommandSource source, @Argument(DISPLAY_ARG) Hologram board) {
    source.sendMessage(board.infoText());
  }

  void updateDisplay(CommandSource source, @Argument(DISPLAY_ARG) Hologram board)
      throws CommandSyntaxException
  {
    if (!board.isSpawned()) {
      throw Messages.render(messagePrefix, "errors.notSpawned")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.update();

    source.sendSuccess(
        Messages.render(messagePrefix, "updated")
            .addValue("board", board.displayName())
            .create(source)
    );
  }
  
  void updateAll(CommandSource source) {
    int updated = 0;

    for (Hologram board : getList(service)) {
      if (!board.isSpawned()) {
        continue;
      }

      updated++;
      board.update();
    }

    source.sendSuccess(
        Messages.render(messagePrefix, "updateAll")
            .addValue("count", updated)
            .create(source)
    );
  }

  void killDisplay(CommandSource source, @Argument(DISPLAY_ARG) Hologram board)
      throws CommandSyntaxException
  {
    if (!board.isSpawned()) {
      throw Messages.render(messagePrefix, "errors.alreadyInactive")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.kill();

    source.sendSuccess(
        Messages.render(messagePrefix, "killed")
            .addValue("board", board)
            .create(source)
    );
  }

  void spawnDisplay(CommandSource source, @Argument(DISPLAY_ARG) Hologram board)
      throws CommandSyntaxException
  {
    if (board.isSpawned()) {
      throw Messages.render(messagePrefix, "errors.alreadySpawned")
          .addValue("board", board.displayName())
          .exception(source);
    }

    board.spawn();

    source.sendSuccess(
        Messages.render(messagePrefix, "spawned")
            .addValue("board", board.displayName())
            .create(source)
    );
  }

  void setLocation(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
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
        Messages.render(messagePrefix, "location")
            .addValue("board", board.displayName())
            .addValue("location", location)
            .create(source)
    );
  }
  
  Component getSetMessage(Audience viewer, Hologram board, String suffix, Object value) {
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

  void setYaw(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
      @Argument("value") float value
  ) {
    board.getDisplayMeta().setYaw(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "yaw", formatNumber(value)));
  }

  void setPitch(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
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
      throw Exceptions.create("No relative coordinates ('~' or '^') allowed here");
    }

    return new Vector3f(
        position.getXCoordinate().value(),
        position.getYCoordinate().value(),
        position.getZCoordinate().value()
    );
  }

  void setScale(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
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
      @Argument(DISPLAY_ARG) Hologram board,
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
      @Argument(DISPLAY_ARG) Hologram board,
      @Argument("value") Billboard billboard
  ) throws CommandSyntaxException {
    board.getDisplayMeta().setBillboard(billboard);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "billboard", billboard));
  }

  void setAlign(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
      @Argument("value") TextAlignment alignment
  ) throws CommandSyntaxException {
    board.getDisplayMeta().setAlign(alignment);
    board.update();

    source.sendSuccess(getSetMessage(source, board, "align", alignment));
  }

  void setBackColor(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
      @Argument(value = "value", optional = true) Color color
  ) {
    board.getDisplayMeta().setBackgroundColor(color);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "backColor", color));
  }

  void setBrightness(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
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
      @Argument(DISPLAY_ARG) Hologram board,
      @Argument("value") boolean value
  ) {
    board.getDisplayMeta().setShadowed(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "shadowed", value));
  }

  void setSeeThrough(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
      @Argument("value") boolean value
  ) {
    board.getDisplayMeta().setSeeThrough(value);
    board.update();
    source.sendSuccess(getSetMessage(source, board, "seeThrough", value));
  }

  void setLineWidth(
      CommandSource source,
      @Argument(DISPLAY_ARG) Hologram board,
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
      @Argument(DISPLAY_ARG) Hologram board,
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
