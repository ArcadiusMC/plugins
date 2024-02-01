package net.arcadiusmc.core.commands.help;

import com.google.common.base.Strings;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.arcadiusmc.command.help.AbstractHelpEntry;
import net.arcadiusmc.command.help.CommandDisplayInfo;
import net.arcadiusmc.command.help.Usage;
import net.arcadiusmc.utils.io.FtcCodecs;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;

@Getter
@RequiredArgsConstructor
class LoadedCommandEntry extends AbstractHelpEntry {

  private static final Codec<String[]> INFO_CODEC
      = Codec.either(Codec.STRING, FtcCodecs.arrayOf(Codec.STRING, String.class))
      .xmap(
          either -> either.map(string -> new String[] {string}, strings -> strings),
          arr -> {
            if (arr.length == 1) {
              return Either.left(arr[0]);
            }
            return Either.right(arr);
          }
      );

  private static final Codec<Usage> USAGE_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.fieldOf("arguments")
                .forGetter(Usage::getArguments),

            Codec.STRING.optionalFieldOf("permission", "")
                .forGetter(Usage::getPermission),

            INFO_CODEC
                .optionalFieldOf("info", new String[0])
                .forGetter(Usage::getInfo)
        )
        .apply(instance, (args, perm, info) -> {
          Usage usage = new Usage(args);
          usage.setPermission(perm);

          for (String s : info) {
            usage.addInfo(s);
          }

          return usage;
        });
  });

  private static final Codec<LoadedCommandEntry> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.listOf().optionalFieldOf("aliases", List.of())
                .forGetter(o -> o.aliases),

            FtcCodecs.COMPONENT.optionalFieldOf("description", Component.empty())
                .forGetter(o -> o.description),

            Codec.STRING.optionalFieldOf("permission", "")
                .forGetter(o -> o.permission),

            Codec.STRING.optionalFieldOf("category", "general")
                .forGetter(o -> o.category),

            USAGE_CODEC.listOf().optionalFieldOf("usages", List.of())
                .forGetter(o -> o.usages)
        )
        .apply(instance, LoadedCommandEntry::new);
  });

  static final Codec<Map<String, LoadedCommandEntry>> MAP_CODEC
      = Codec.unboundedMap(Codec.STRING, CODEC);

  private final List<String> aliases;
  private final Component description;
  private final String permission;
  private final String category;
  private final List<Usage> usages;

  @Setter
  private String label;

  @Override
  public CommandDisplayInfo createDisplay() {
    return new CommandDisplayInfo(
        label,
        permission,
        description,
        source -> true,
        usages,
        aliases,
        category
    );
  }

  @Override
  public String getCategory() {
    return category;
  }

  @Override
  public boolean test(CommandSource source) {
    if (Strings.isNullOrEmpty(permission)) {
      return true;
    }
    return source.hasPermission(permission);
  }
}
