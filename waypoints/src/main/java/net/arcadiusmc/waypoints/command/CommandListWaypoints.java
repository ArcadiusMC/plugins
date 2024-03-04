package net.arcadiusmc.waypoints.command;

import static net.kyori.adventure.text.Component.text;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.page.Footer;
import net.arcadiusmc.text.page.PageEntry;
import net.arcadiusmc.text.page.PageFormat;
import net.arcadiusmc.text.page.PagedIterator;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointManager;
import net.arcadiusmc.waypoints.WaypointProperties;
import net.arcadiusmc.waypoints.menu.WaypointListPage;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandListWaypoints extends BaseCommand {

  final PageFormat<Waypoint> waypointPageFormat;

  private static final int DEFAULT_PAGE_SIZE = 10;

  public CommandListWaypoints() {
    super("ListWaypoints");

    setPermission(WPermissions.WAYPOINTS);
    setDescription("Lists named waypoints");
    setAliases("listregions", "regions", "regionlist", "waypointlist");

    this.waypointPageFormat = createPageFormat();

    register();
  }

  private PageFormat<Waypoint> createPageFormat() {
    PageFormat<Waypoint> format = PageFormat.create();
    format.setHeader(text("Named waypoints"));

    format.getHeader().append((it, writer, context) -> {
      writer.newLine();
      writer.write(
          text("[Open List menu]", NamedTextColor.AQUA)
              .clickEvent(ClickEvent.runCommand("/waypointgui"))
              .hoverEvent(Messages.CLICK_ME.renderText(writer.viewer()))
      );
    });

    format.setFooter(
        Footer.create().setPageButton("/" + getName() + " %s %s")
    );

    PageEntry<Waypoint> entry = PageEntry.create();
    entry.setEntryDisplay((writer, waypoint, viewerIndex, context, it) -> {
      Component displayName = waypoint.displayName();
      writer.write(displayName);
    });

    entry.setIndex((viewerIndex, waypoint, it) -> {
      return text(viewerIndex + ")", NamedTextColor.GRAY);
    });

    format.setEntry(entry);
    return format;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("", "Lists named waypoints");

    factory.usage("<page: number(1..)> [<page size: number(5..20)>]")
        .addInfo("Lists named waypoints on a specific <page>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> listWaypoints(c.getSource(), 1, DEFAULT_PAGE_SIZE))

        .then(argument("page", IntegerArgumentType.integer(1))
            .executes(c -> {
              int page = c.getArgument("page", Integer.class);
              return listWaypoints(c.getSource(), page, DEFAULT_PAGE_SIZE);
            })

            .then(argument("pageSize" , IntegerArgumentType.integer(5, 20))
                .executes(c -> {
                  int page = c.getArgument("page", Integer.class);
                  int pageSize = c.getArgument("pageSize", Integer.class);
                  return listWaypoints(c.getSource(), page, pageSize);
                })
            )
        );
  }

  private int listWaypoints(CommandSource source, int page, int pageSize)
      throws CommandSyntaxException
  {
    WaypointManager manager = WaypointManager.getInstance();
    List<Waypoint> waypoints;

    if (source.isPlayer()) {
      User user = Commands.getUserSender(source);
      waypoints = WaypointListPage.getList(user);
    } else {
      waypoints = new ObjectArrayList<>(manager.getWaypoints());
      waypoints.removeIf(waypoint -> Strings.isNullOrEmpty(waypoint.getEffectiveName()));
    }

    // Remove private waypoints, and ones with no effective names
    waypoints.removeIf(waypoint -> {
      return !waypoint.get(WaypointProperties.PUBLIC);
    });

    Commands.ensurePageValid(--page, pageSize, waypoints.size());
    PagedIterator<Waypoint> it = PagedIterator.of(waypoints, page, pageSize);

    TextWriter writer = TextWriters.newWriter();
    writer.viewer(source);

    waypointPageFormat.write(it, writer, Context.EMPTY);

    Component text = writer.asComponent();
    source.sendMessage(text);

    return 0;
  }
}