package net.arcadiusmc.core.commands.help;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.HelpEntry;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.forthecrown.grenadier.CommandSource;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.ViewerAwareMessage;

@Getter @Setter
public class LoadedHelpEntry implements HelpEntry {

  private final Set<String> labels;
  private final String mainLabel;

  private final ViewerAwareMessage shortText;
  private final ViewerAwareMessage fullText;

  private BaseCommand command;

  public LoadedHelpEntry(
      Set<String> labels,
      String label,
      ViewerAwareMessage shortText,
      ViewerAwareMessage fullText
  ) {
    this.mainLabel = label;
    this.labels = Collections.unmodifiableSet(labels);

    this.shortText = shortText;
    this.fullText = fullText;
  }

  @Override
  public void writeShort(TextWriter writer, CommandSource source) {
    writer.fieldSameLine("/" + mainLabel, shortText);
  }

  @Override
  public void writeFull(TextWriter writer, CommandSource source) {
    writer.write(Placeholders.render(fullText.create(source)));
  }

  @Override
  public Collection<String> getKeywords() {
    return labels;
  }

  @Override
  public boolean test(CommandSource source) {
    return true;
  }
}
