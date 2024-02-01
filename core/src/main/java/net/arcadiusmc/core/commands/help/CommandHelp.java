package net.arcadiusmc.core.commands.help;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.ArcadiusHelpList;
import net.arcadiusmc.command.help.HelpEntry;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.page.Footer;
import net.arcadiusmc.text.page.Header;
import net.arcadiusmc.text.page.PageEntry;
import net.arcadiusmc.text.page.PageFormat;
import net.arcadiusmc.text.page.PagedIterator;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

public class CommandHelp extends BaseCommand {

  private static final int DEF_PAGE_SIZE = 10;


  // Context used to pass data onto the page formatter
  private final ContextSet contextSet = ContextSet.create();

  private final ContextOption<CommandSource> sourceOption
      = contextSet.newOption();

  private final ContextOption<String> inputOption
      = contextSet.newOption();

  private final ContextOption<Integer> actualPageSize
      = contextSet.newOption(5);

  // Page format used to format entries for list-based display
  private final PageFormat<HelpEntry> pageFormat = PageFormat.create();

  private final PageFormat<Component> singleEntryPaginator
      = PageFormat.create();

  public CommandHelp() {
    super("Help");

    initPaginator();

    setPermission(Permissions.HELP);
    setDescription("Displays help information");
    setAliases("?");

    register();
  }

  // Initialize the page format used to display help entries
  void initPaginator() {

    // Footer format
    var footer = Footer.create()
        .setPageButton((viewerPage, pageSize, context) -> {
          var s = context.get(inputOption);

          return ClickEvent.runCommand(
              String.format("/help '%s' %s %s",
                  s == null ? "" : s,
                  viewerPage, context.get(actualPageSize)
              )
          );
        });

    // Header format
    Header header = Header.create();
    header.title((it, writer, context) -> {
      var s = context.get(inputOption);

      if (Strings.isNullOrEmpty(s)) {
        writer.write("Help");
      } else {
        writer.formatted("Results for: {0}", s);
      }
    });

    // Entry format
    PageEntry<HelpEntry> entry = PageEntry.create();
    entry.setEntryDisplay((writer, entry1, viewerIndex, context, it) -> {
      entry1.writeShort(writer, context.getOrThrow(sourceOption));
    });

    PageEntry<Component> singletonEntry = PageEntry.create();
    singletonEntry.setIndex((viewerIndex, entry1, it) -> null);
    singletonEntry.setEntryDisplay((writer, entry1, viewerIndex, context, it) -> {
      writer.line(entry1);
    });

    singleEntryPaginator.setFooter(footer);
    singleEntryPaginator.setHeader(header);
    singleEntryPaginator.setEntry(singletonEntry);
    pageFormat.setHeader(header);
    pageFormat.setFooter(footer);
    pageFormat.setEntry(entry);
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("")
        .addInfo("Displays all help info");

    factory.usage("<topic> [<page: number(1..)>] [<page size: number(5..20)>]")
        .addInfo("Queries information for a specific topic.")
        .addInfo("[page] optionally displays the specific page of information");

    factory.usage("all [<page: number(1..)>] [<page size: number(5..20)>]")
        .addInfo("Displays a specific page of all help info.");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> help(c, false, false, false))

        .then(argument("string", new HelpArgument())
            .executes(c -> help(c, true, false, false))

            .then(argument("page", IntegerArgumentType.integer(1))
                .executes(c -> help(c, true, true, false))

                .then(argument("pageSize", IntegerArgumentType.integer(5, 20))
                    .executes(c -> help(c, true, true, true))
                )
            )
        );
  }

  private int help(
      CommandContext<CommandSource> c,
      boolean inputGiven,
      boolean pageGiven,
      boolean sizeGiven
  ) throws CommandSyntaxException {
    int page = 0;
    int pageSize = DEF_PAGE_SIZE;
    String input = "";

    if (inputGiven) {
      input = c.getArgument("string", String.class);
    }

    if (pageGiven) {
      page = c.getArgument("page", Integer.class) - 1;
    }

    if (sizeGiven) {
      pageSize = c.getArgument("pageSize", Integer.class);
    }

    CommandSource source = c.getSource();
    Component component = queryText(source, input, page, pageSize);

    if (component == null) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    c.getSource().sendMessage(component);
    return 0;
  }

  private Component queryText(CommandSource source, String tag, int page, int pageSize)
      throws CommandSyntaxException {
    var entries = ArcadiusHelpList.helpList().query(source, tag);

    var writer = TextWriters.newWriter();
    writer.setFieldStyle(Style.style(NamedTextColor.YELLOW));
    writer.placeholders(Placeholders.newRenderer().useDefaults());

    Context context = contextSet.createContext()
        .set(sourceOption, source)
        .set(inputOption, tag)
        .set(actualPageSize, pageSize);

    // Single entry, write that 1 entry
    if (entries.size() == 1) {
      pageSize += 5;
      var entry = entries.iterator().next();
      var loreWriter = TextWriters.buffered();

      loreWriter.setFieldStyle(Style.style(NamedTextColor.YELLOW));
      entry.writeFull(loreWriter, source);

      var text = loreWriter.getBuffer();

      // Ensure list isn't empty and page number is valid
      Commands.ensurePageValid(page, pageSize, text.size());

      var it = PagedIterator.of(text, page, pageSize);
      singleEntryPaginator.write(it, writer, context);

      return writer.asComponent();
    }

    // Ensure list isn't empty and page number is valid
    Commands.ensurePageValid(page, pageSize, entries.size());

    // Format all results onto a page
    var iterator = PagedIterator.of(entries, page, pageSize);

    pageFormat.write(iterator, writer, context);
    return writer.asComponent();
  }
}
