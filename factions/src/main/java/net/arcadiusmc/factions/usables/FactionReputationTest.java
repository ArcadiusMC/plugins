package net.arcadiusmc.factions.usables;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionManager;
import net.arcadiusmc.factions.FactionMember;
import net.arcadiusmc.factions.Factions;
import net.arcadiusmc.factions.FactionsConfig;
import net.arcadiusmc.factions.commands.FactionArgument;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableCodecs;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class FactionReputationTest implements Condition {

  private static final Logger LOGGER = Loggers.getLogger();

  final String factionKey;
  final IntRange reputationRange;

  public FactionReputationTest(String factionKey, IntRange reputationRange) {
    this.factionKey = factionKey;
    this.reputationRange = reputationRange;
  }

  @Override
  public boolean test(Interaction interaction) {
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      return false;
    }

    Player player = playerOpt.get();
    FactionManager manager = Factions.getManager();
    Faction faction = manager.getFaction(factionKey);

    if (faction == null) {
      LOGGER.error("Unknown faction '{}' in usable {}", factionKey, interaction.getObject());
      return false;
    }

    FactionMember member = faction.getActiveMember(player.getUniqueId());

    if (member == null) {
      return false;
    }

    if (reputationRange == IntRange.UNLIMITED || reputationRange == null) {
      return true;
    }

    int reputation = member.getReputation();
    return reputationRange.contains(reputation);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    Player viewer = interaction.getPlayer().orElse(null);
    Faction faction = Factions.getManager().getFaction(factionKey);

    Component factionName = Optional.ofNullable(faction)
        .map(f -> f.displayName(viewer))
        .orElse(Component.text(factionKey));

    String messageKey;

    if (reputationRange == IntRange.UNLIMITED || reputationRange == null) {
      messageKey = "factions.errors.notFactionMember";
    } else {
      messageKey = "factions.errors.tooLittleReputation";
    }

    return Messages.render(messageKey)
        .addValue("faction", factionName)
        .create(viewer);
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(factionKey);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return FactionMemberType.INSTANCE;
  }
}

enum FactionMemberType implements ObjectType<FactionReputationTest> {
  INSTANCE;

  static final Codec<FactionReputationTest> CODEC = Codec
      .either(
          RecordCodecBuilder.<FactionReputationTest>create(instance -> {
            return instance
                .group(
                    ExtraCodecs.KEY_CODEC.fieldOf("faction_key")
                        .forGetter(o -> o.factionKey),

                    UsableCodecs.INT_RANGE.optionalFieldOf("range")
                        .forGetter(o -> Optional.ofNullable(o.reputationRange))
                )
                .apply(instance, (s, intRange) -> {
                  return new FactionReputationTest(s, intRange.orElse(IntRange.UNLIMITED));
                });
          }),

          ExtraCodecs.KEY_CODEC
              .xmap(s -> new FactionReputationTest(s, IntRange.UNLIMITED), t -> t.factionKey)
      )
      .xmap(
          either -> either.map(Function.identity(), Function.identity()),
          test -> {
            if (test.reputationRange == IntRange.UNLIMITED || test.reputationRange == null) {
              return Either.right(test);
            }

            return Either.left(test);
          }
      );

  @Override
  public FactionReputationTest parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    Faction faction = FactionArgument.FACTION.parse(reader);
    IntRange range;

    if (reader.canRead()) {
      reader.expect(' ');
      IntRange parsedRange = ArgumentTypes.intRange().parse(reader);

      if (parsedRange.isExact()) {
        range = IntRange.atLeast(parsedRange.min().getAsInt());
      } else {
        range = parsedRange;
      }
    } else {
      range = IntRange.UNLIMITED;
    }

    return new FactionReputationTest(faction.getKey(), range);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    String token = builder.getRemainingLowerCase();
    int spaceIndex = token.indexOf(' ');

    if (spaceIndex != -1) {
      builder = builder.createOffset(spaceIndex);

      FactionsConfig config = Factions.getConfig();
      int min = config.getMinReputation();
      int max = config.getMaxReputation();

      return Completions.suggest(builder,
          ".." + max,
          min + "..",
          min + ".." + max
      );
    }

    return FactionArgument.FACTION.listSuggestions(context, builder);
  }

  @Override
  public <S> DataResult<FactionReputationTest> load(Dynamic<S> dynamic) {
    return CODEC.parse(dynamic);
  }

  @Override
  public <S> DataResult<S> save(@NotNull FactionReputationTest value, @NotNull DynamicOps<S> ops) {
    return CODEC.encodeStart(ops, value);
  }
}