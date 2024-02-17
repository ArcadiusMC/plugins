package net.arcadiusmc.mail.command;

import java.util.List;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.arcadiusmc.mail.Mail;
import net.arcadiusmc.mail.Page;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.page.Footer;
import net.arcadiusmc.text.page.Header;
import net.arcadiusmc.text.page.PageEntry;
import net.arcadiusmc.text.page.PageFormat;
import net.arcadiusmc.text.page.PagedIterator;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

class MailList {

  public static final ContextSet SET = ContextSet.create();

  public static final ContextOption<User> MAIL_USER = SET.newOption();
  public static final ContextOption<Page> PAGE = SET.newOption();
  public static final ContextOption<Boolean> SELF = SET.newOption();

  static final PageFormat<Mail> PAGE_FORMAT;

  static {
    PageFormat<Mail> format = PageFormat.create();

    format.setHeader(
        Header.<Mail>create()
            .title((it, writer, context) -> {
              String messageKey = context.getOrThrow(SELF)
                  ? "mail.listHeader.self"
                  : "mail.listHeader.other";

              writer.write(
                  Messages.render(messageKey)
                      .addValue("player", context.getOrThrow(MAIL_USER))
                      .create(writer.viewer())
              );
            })
    );

    format.setEntry(PageEntry.of((writer, entry, viewerIndex, context, it) -> {
      Page p = context.getOrThrow(PAGE);
      var display = entry.displayText(writer.viewer(), p);
      writer.write(display);
    }));

    format.setFooter(
        Footer.create().setPageButton((viewerPage, pageSize, context) -> {
          String cmdFormat;
          boolean self = context.getOrThrow(SELF);
          User user = context.getOrThrow(MAIL_USER);

          if (self) {
            cmdFormat = "/mail %s %s";
          } else {
            cmdFormat = "/mail read_other " + user.getName() + " %s %s";
          }

          return ClickEvent.runCommand(
              cmdFormat.formatted(viewerPage, pageSize)
          );
        })
    );

    PAGE_FORMAT = format;
  }

  static Component formatMail(CommandSource viewer, Page page, List<Mail> mailList) {
    var target = page.player();

    boolean self = viewer.textName().equals(target.getName());
    PagedIterator<Mail> it = PagedIterator.reversed(mailList, page.page() - 1, page.pageSize());

    Context ctx = SET.createContext();
    ctx.set(MAIL_USER, target);
    ctx.set(SELF, self);
    ctx.set(PAGE, page);

    var writer = TextWriters.newWriter();
    writer.viewer(viewer);

    PAGE_FORMAT.write(it, writer, ctx);

    return writer.asComponent();
  }

}
