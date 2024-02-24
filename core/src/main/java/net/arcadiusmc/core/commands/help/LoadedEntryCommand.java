package net.arcadiusmc.core.commands.help;

import java.util.HashSet;
import java.util.Set;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.text.TextWriters;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

public class LoadedEntryCommand extends BaseCommand {

  private final LoadedHelpEntry entry;

  public LoadedEntryCommand(String name, LoadedHelpEntry entry) {
    super(name);
    this.entry = entry;

    Set<String> labels = new HashSet<>(entry.getLabels());
    labels.remove(name);

    setAliases(labels);
    getCommand().withDescription(entry.getShortText().asComponent());
    setPermission(Permissions.HELP);
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      var writer = TextWriters.newWriter();
      writer.setFieldStyle(Style.style(NamedTextColor.YELLOW));
      writer.viewer(c.getSource());

      entry.writeFull(writer, c.getSource());

      c.getSource().sendMessage(writer.asComponent());
      return 0;
    });
  }
}
