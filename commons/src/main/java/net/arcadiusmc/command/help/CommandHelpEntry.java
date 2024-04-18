package net.arcadiusmc.command.help;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.text.DefaultTextWriter;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.apache.commons.lang3.ArrayUtils;

@Data
public class CommandHelpEntry implements HelpEntry {

  private String label;
  private List<String> aliases = new ArrayList<>();
  private Component description = Component.empty();
  private List<Usage> usages = new ArrayList<>();
  private String category = "";
  private String permission = "";

  public static String packageNameToCategory(String packageName) {
    return packageName
        .replaceAll("net\\.arcadiusmc\\.?", "")
        .replaceAll("\\.?commands?\\.?", "")
        .replace("core", "")
        .replace('.', '/');
  }

  @Override
  public String getMainLabel() {
    return label;
  }

  @Override
  public void writeShort(TextWriter writer, CommandSource source) {
    HoverEvent<Component> hover = asHover(source);

    writer.formatted("/{0}", writer.getFieldStyle().hoverEvent(hover), label);

    if (!Text.isEmpty(description)) {
      writer.write(writer.getFieldSeparator().hoverEvent(hover));
      writer.write(description.style(writer.getFieldValueStyle().hoverEvent(hover)));
    }
  }

  @Override
  public void writeFull(TextWriter writer, CommandSource source) {
    writeMetadata(writer, source);
    writeUsages(writer, source, true);
  }

  @Override
  public Collection<String> getKeywords() {
    Set<String> strings = new HashSet<>(aliases);
    strings.add(label);
    return strings;
  }

  @Override
  public boolean test(CommandSource source) {
    if (Strings.isNullOrEmpty(label)) {
      return false;
    }

    if (Strings.isNullOrEmpty(permission)) {
      return true;
    }
    return source.hasPermission(permission);
  }

  public List<Usage> usagesFor(CommandSource source) {
    return usages.stream()
        .filter(usage -> usage.getCondition().test(source))
        .toList();
  }

  private String usagePrefix() {
    return "/" + label;
  }

  public HoverEvent<Component> asHover(CommandSource source) {
    DefaultTextWriter writer = TextWriters.newWriter();
    writer.setFieldStyle(Style.style(NamedTextColor.GRAY));

    writer.formatted("/{0}", label);
    writeMetadata(writer, source);

    List<Usage> usages = usagesFor(source);

    if (!usages.isEmpty()) {
      writer.field("Usages", "");
      usages.forEach(usage -> writer.line(usage.argumentsWithPrefix(usagePrefix())));
    }

    return writer.asComponent().asHoverEvent();
  }

  public void writeMetadata(TextWriter writer, CommandSource source) {
    if (!test(source)) {
      return;
    }

    if (aliases != null && !aliases.isEmpty()) {
      writer.field("Aliases", Joiner.on(", ").join(aliases));
    }

    if (description != null) {
      writer.field("Description", description);
    }

    if (source.hasPermission(Permissions.ADMIN_HELP_INFO)) {
      writer.field("Permission", Strings.isNullOrEmpty(permission) ? "unset" : permission);

      if (!Strings.isNullOrEmpty(category)) {
        writer.field("Category", category);
      }
    }
  }

  public void writeUsages(TextWriter writer, CommandSource source, boolean includeTitle) {
    if (!test(source)) {
      return;
    }

    List<Usage> usages = usagesFor(source);

    if (usages.isEmpty()) {
      return;
    }

    if (includeTitle) {
      writer.newLine();
      writer.newLine();
      writer.field("Usages", "");
    }

    for (Usage n : usages) {
      writer.line(n.argumentsWithPrefix(usagePrefix()));

      if (!ArrayUtils.isEmpty(n.getInfo())) {
        writer.write(":");
        Arrays.stream(n.getInfo()).forEach(s -> writer.line("  " + s, NamedTextColor.GRAY));
      }
    }
  }
}
