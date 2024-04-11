package net.arcadiusmc.core.placeholder;

import com.google.common.base.Strings;
import java.util.List;
import net.arcadiusmc.command.help.ArcadiusHelpList;
import net.arcadiusmc.command.help.HelpEntry;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class HelpTopicPlaceholder implements TextPlaceholder {

  @Override
  public @Nullable Component render(String match, PlaceholderContext render) {
    if (Strings.isNullOrEmpty(match)) {
      return null;
    }

    ArcadiusHelpList list = ArcadiusHelpList.helpList();
    CommandSource source = Grenadier.createSource(Bukkit.getConsoleSender());

    List<HelpEntry> entries = list.query(source, match);
    if (entries.size() != 1) {
      return null;
    }

    HelpEntry entry = entries.get(0);
    TextWriter writer = TextWriters.newWriter();

    writer.viewer(render.viewer());
    entry.writeFull(writer, source);

    return writer.asComponent();
  }
}
