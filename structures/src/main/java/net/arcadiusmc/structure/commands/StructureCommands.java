package net.arcadiusmc.structure.commands;

import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockProcessors;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.StructureEntitySpawner;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.StructuresPlugin;
import net.arcadiusmc.structure.buffer.BlockBuffers;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Suggester;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ParsedPosition;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.FlagOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.OptionsArgumentBuilder;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import org.spongepowered.math.vector.Vector3d;

public final class StructureCommands {

  static final ArgumentOption<Rotation> ROT_ARG
      = Options.argument(ArgumentTypes.enumType(Rotation.class))
      .setLabel("rotation")
      .setDefaultValue(Rotation.NONE)
      .build();

  private static final ArgumentOption<Vector3d> OFFSET_ARG
      = Options.argument(new VectorParser())
      .setLabel("offset")
      .setDefaultValue(Vector3d.ZERO)
      .build();

  private static final ArgumentOption<Vector3d> PIVOT_ARG
      = Options.argument(new VectorParser())
      .setLabel("pivot")
      .setDefaultValue(Vector3d.ZERO)
      .build();

  private static final ArgumentOption<ParsedPosition> POS_ARG
      = Options.argument(ArgumentTypes.blockPosition())
      .setLabel("position")
      .setDefaultValue(ParsedPosition.IDENTITY)
      .build();

  static final FlagOption PLACE_ENTITIES = Options.flag("place-entities");
  static final FlagOption IGNORE_AIR = Options.flag("ignore-air");

  public static void createCommands(StructuresPlugin plugin) {
    new CommandStructure(plugin);
    new CommandStructFunction();
  }

  static OptionsArgumentBuilder createPlacementOptions() {
    return OptionsArgument.builder()
        .addOptional(OFFSET_ARG)
        .addOptional(ROT_ARG)
        .addOptional(POS_ARG)
        .addOptional(PIVOT_ARG)
        .addFlag(PLACE_ENTITIES)
        .addFlag(IGNORE_AIR);
  }

  static ArgumentOption<String> createPaletteArg(String structureArgument) {
    return Options.argument(Arguments.RESOURCE_KEY)
        .setLabel("palette")
        .setDefaultValue(BlockStructure.DEFAULT_PALETTE_NAME)
        .setSuggester(createPaletteSuggestions(structureArgument))
        .build();
  }

  static Suggester<CommandSource> createPaletteSuggestions(String argumentName) {
    return (context, builder) -> {
      Holder<BlockStructure> struct = context.getArgument(argumentName, Holder.class);
      return Completions.suggest(builder, struct.getValue().getPalettes().keySet());
    };
  }

  static void configurePlacement(
      StructurePlaceConfig.Builder builder,
      CommandSource source,
      ParsedOptions options,
      ArgumentOption<String> paletteName
  ) {
    builder.buffer(BlockBuffers.immediate(source.getWorld()));

    options.getValueOptional(POS_ARG)
        .ifPresentOrElse(
            position -> {
              builder.pos(Vectors.intFrom(position.apply(source)));
            },
            () -> {
              builder.pos(Vectors.intFrom(source.getLocation()));
            }
        );

    Vector3d pivot = options.getValue(PIVOT_ARG);
    Vector3d offset = options.getValue(OFFSET_ARG);
    Rotation rotation = options.getValue(ROT_ARG);

    Transform transform = builder.transform()
        .withPivot(pivot)
        .addOffset(offset)
        .addRotation(rotation);

    builder.transform(transform);

    if (options.has(PLACE_ENTITIES)) {
      builder.entitySpawner(StructureEntitySpawner.world(source.getWorld()));
    }

    if (options.has(IGNORE_AIR)) {
      builder.addProcessor(BlockProcessors.IGNORE_AIR);
    }

    if (paletteName != null && options.has(paletteName)) {
      builder.paletteName(options.getValue(paletteName));
    }
  }
}
