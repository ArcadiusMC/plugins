package net.arcadiusmc.core.commands.help;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.command.help.CommandHelpEntry;
import net.arcadiusmc.command.help.Usage;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;

@Getter
@RequiredArgsConstructor
class CommandEntryLoader {

  private static final Codec<String[]> INFO_CODEC
      = Codec.either(Codec.STRING, ExtraCodecs.arrayOf(Codec.STRING, String.class))
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

  private static final Codec<CommandHelpEntry> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.listOf().optionalFieldOf("aliases", List.of())
                .forGetter(CommandHelpEntry::getAliases),

            ExtraCodecs.COMPONENT.optionalFieldOf("description", Component.empty())
                .forGetter(CommandHelpEntry::getDescription),

            Codec.STRING.optionalFieldOf("permission", "")
                .forGetter(CommandHelpEntry::getPermission),

            Codec.STRING.optionalFieldOf("category", "general")
                .forGetter(CommandHelpEntry::getCategory),

            USAGE_CODEC.listOf().optionalFieldOf("usages", List.of())
                .forGetter(CommandHelpEntry::getUsages)
        )
        .apply(instance, (aliases, desc, permission, category, usages) -> {
          CommandHelpEntry entry = new CommandHelpEntry();
          entry.setAliases(aliases);
          entry.setDescription(desc);
          entry.setPermission(permission);
          entry.setCategory(category);
          entry.setUsages(usages);
          return entry;
        });
  });

  static final Codec<Map<String, CommandHelpEntry>> MAP_CODEC
      = Codec.unboundedMap(Codec.STRING, CODEC);
}
