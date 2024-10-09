package net.arcadiusmc.structure.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.arcadiusmc.structure.BlockStructure.DEFAULT_PALETTE_NAME;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.arcadiusmc.WorldEditHook;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.DataCommands;
import net.arcadiusmc.command.DataCommands.DataAccessor;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockPalette;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.StructureFillConfig;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.StructurePlaceConfig.Builder;
import net.arcadiusmc.structure.Structures;
import net.arcadiusmc.structure.StructuresPlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.utils.math.AreaSelection;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.grenadier.CommandContexts;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.FlagOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.spongepowered.math.vector.Vector3i;

public class CommandStructure extends BaseCommand {

  static final String STRUCTURE_ARG = "structure";

  /* ----------------------------- CREATION ARGUMENTS ------------------------------ */

  private static final ArgumentOption<List<EntityType>> IGNORE_ENT_ARG
      = Options.argument(ArgumentTypes.array(ArgumentTypes.enumType(EntityType.class)))
      .setLabel("ignore-entities")
      .setDefaultValue(Collections.emptyList())
      .build();

  private static final ArgumentOption<List<BlockFilterArgument.Result>> BLOCK_FILTER
      = Options.argument(ArgumentTypes.array(ArgumentTypes.blockFilter()))
      .setLabel("ignore-blocks")
      .setDefaultValue(Collections.emptyList())
      .build();

  private static final FlagOption INCLUDE_FUNCTIONS
      = Options.flag("include-functions");

  private static final OptionsArgument FILL_ARGS = OptionsArgument.builder()
      .addOptional(BLOCK_FILTER)
      .addOptional(IGNORE_ENT_ARG)
      .addFlag(INCLUDE_FUNCTIONS)
      .build();

  private static final ArgumentOption<String> PALETTE_NAME
      = StructureCommands.createPaletteArg(STRUCTURE_ARG);

  private static final DataAccessor HEADER_ACCESSOR = new DataAccessor() {
    @Override
    public CompoundTag getTag(CommandContext<CommandSource> context) {
      Holder<BlockStructure> holder = context.getArgument(STRUCTURE_ARG, Holder.class);

      return holder.getValue().getHeader()
          .copy()
          .asCompound();
    }

    @Override
    public void setTag(CommandContext<CommandSource> context, CompoundTag tag) {
      Holder<BlockStructure> holder = context.getArgument(STRUCTURE_ARG, Holder.class);

      CompoundTag header = holder.getValue().getHeader();
      header.clear();
      header.merge(tag);
    }
  };

  private final StructuresPlugin plugin;

  private final ArgumentType<Holder<BlockStructure>> argumentType;
  private final OptionsArgument placementOptions;

  public CommandStructure(StructuresPlugin plugin) {
    super("struct");

    this.plugin = plugin;
    this.argumentType = new RegistryArguments<>(plugin.getStructures().getRegistry(), "Structure");
    this.placementOptions = StructureCommands.createPlacementOptions()
        .addOptional(PALETTE_NAME)
        .build();

    setAliases("structure", "ftc-struct");
    setDescription("Structure command");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("save")
            .executes(c -> {
              plugin.getStructures().save();
              c.getSource().sendSuccess(Messages.renderText("structures.saved", c.getSource()));
              return SINGLE_SUCCESS;
            })
        )
        .then(literal("reload-structures")
            .executes(c -> {
              plugin.getStructures().load();

              c.getSource().sendSuccess(
                  Messages.renderText("structures.reloaded.structures", c.getSource())
              );
              return SINGLE_SUCCESS;
            })
        )
        .then(literal("reload-config")
            .executes(c -> {
              plugin.reloadConfig();

              c.getSource().sendSuccess(
                  Messages.renderText("structures.reloaded.config", c.getSource())
              );
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("create")
            .then(argument("name", Arguments.RESOURCE_KEY)
                .executes(this::fillStructure)

                .then(argument("options", FILL_ARGS)
                    .executes(this::fillStructure)
                )
            )
        )

        .then(literal("redefine")
            .then(structureArg()
                .executes(this::fillStructure)

                .then(argument("options", FILL_ARGS)
                    .executes(this::fillStructure)
                )
            )
        )

        .then(literal("remove")
            .then(structureArg()
                .executes(c -> {
                  Holder<BlockStructure> holder = getStructure(c);

                  Structures structures = plugin.getStructures();
                  structures.getRegistry().remove(holder.getId());

                  c.getSource().sendSuccess(
                      Messages.render("structures.removed")
                          .addValue("structure", holder.getKey())
                          .create(c.getSource())
                  );
                  return SINGLE_SUCCESS;
                })
            )
        )

        .then(literal("place")
            .then(structureArg()
                .executes(this::placeStructure)

                .then(argument("options", placementOptions)
                    .executes(this::placeStructure)
                )
            )
        )

        .then(literal("palettes")
            .then(literal("list")
                .then(structureArg()
                    .executes(this::listPalettes)
                )
            )

            .then(literal("add")
                .then(argument("palette", Arguments.RESOURCE_KEY)
                    .suggests(StructureCommands.createPaletteSuggestions(STRUCTURE_ARG))
                    .executes(this::fillStructure)

                    .then(argument("optinos", FILL_ARGS)
                        .executes(this::fillStructure)
                    )
                )
            )

            .then(literal("remove")
                .then(structureArg()
                    .then(argument("palette", Arguments.RESOURCE_KEY)
                        .suggests(StructureCommands.createPaletteSuggestions(STRUCTURE_ARG))
                        .executes(this::removePalette)
                    )
                )
            )
        )

        .then(literal("header").then(createDataArgument()));
  }

  private int removePalette(CommandContext<CommandSource> context) throws CommandSyntaxException {
    String paletteName = context.getArgument("palette", String.class);
    Holder<BlockStructure> holder = getStructure(context);

    if (paletteName.equals(DEFAULT_PALETTE_NAME)) {
      throw Messages.render("structures.errors.cannotRemoveDefault")
          .exception(context.getSource());
    }

    holder.getValue().getPalettes().remove(paletteName);

    context.getSource().sendSuccess(
        Messages.render("structures.palettes.removed")
            .addValue("structure", holder.getKey())
            .addValue("palette", paletteName)
            .create(context.getSource())
    );
    return SINGLE_SUCCESS;
  }

  private int listPalettes(CommandContext<CommandSource> c) throws CommandSyntaxException {
    Holder<BlockStructure> holder = getStructure(c);
    Map<String, BlockPalette> palettes = holder.getValue().getPalettes();

    TextWriter writer = TextWriters.newWriter();
    writer.viewer(c.getSource());

    writer.line(
        Messages.render("structures.palettes.list.header")
            .addValue("structure", holder.getKey())
            .create(c.getSource())
    );

    for (String s : palettes.keySet()) {
      writer.line(
          Messages.render("structures.palettes.list.format")
              .addValue("palette", s)
              .create(c.getSource())
      );
    }

    c.getSource().sendMessage(writer.asComponent());
    return SINGLE_SUCCESS;
  }

  private int fillStructure(CommandContext<CommandSource> c) throws CommandSyntaxException {
    Map<String, ParsedArgument<CommandSource, ?>> arguments = CommandContexts.getArguments(c);
    CommandSource source = c.getSource();

    Registry<BlockStructure> registry = plugin.getStructures().getRegistry();

    final int ft_palette_add = 0;
    final int ft_create = 1;
    final int ft_redefine = 2;

    BlockStructure structure;
    String paletteName;
    String registryKey;
    int fillType;

    if (arguments.containsKey("palette")) {
      Holder<BlockStructure> holder = getStructure(c);
      structure = holder.getValue();
      paletteName = c.getArgument("palette", String.class);
      registryKey = holder.getKey();
      fillType = ft_palette_add;

      if (structure.getPalette(paletteName) != null) {
        throw Messages.render("structures.errors.paletteAlreadyUsed")
            .addValue("palette", paletteName)
            .exception(source);
      }
    } else if (arguments.containsKey("name")) {
      structure = new BlockStructure();
      paletteName = DEFAULT_PALETTE_NAME;
      registryKey = c.getArgument("name", String.class);
      fillType = ft_create;

      if (registry.contains(registryKey)) {
        throw Messages.render("structures.errors.alreadyExists")
            .addValue("structure", registry)
            .exception(source);
      }
    } else {
      Holder<BlockStructure> holder = getStructure(c);
      structure = new BlockStructure();
      paletteName = DEFAULT_PALETTE_NAME;
      registryKey = holder.getKey();
      fillType = ft_redefine;

      registry.remove(holder.getId());
    }

    Player player = source.asPlayer();
    AreaSelection selection = WorldEditHook.getSelectedBlocks(player);

    if (selection == null) {
      throw Exceptions.NO_REGION_SELECTION;
    }

    // Ensure palette is not a different size from default palette
    if (!paletteName.equals(DEFAULT_PALETTE_NAME)
        && !structure.getDefaultSize().equals(Vector3i.ZERO)
        && !selection.size().equals(structure.getDefaultSize())
    ) {
      throw Messages.render("structures.errors.invalidSize")
          .addValue("selectionSize", selection.size())
          .addValue("correctSize", structure.getDefaultSize())
          .exception(source);
    }

    Predicate<Block> blockFilter = block -> block.getType() != Material.STRUCTURE_VOID;
    Predicate<Entity> entityFilter = entity -> entity.getType() != EntityType.PLAYER;

    ParsedOptions args;
    if (arguments.containsKey("options")) {
      args = ArgumentTypes.getOptions(c, "options");
    } else {
      args = ParsedOptions.EMPTY;
    }

    if (args.has(BLOCK_FILTER)) {
      var list = args.getValue(BLOCK_FILTER);

      blockFilter = blockFilter.and(block -> {
        for (var r : list) {
          if (r.test(block)) {
            return false;
          }
        }

        return true;
      });
    }

    if (args.has(IGNORE_ENT_ARG)) {
      List<EntityType> list = args.getValue(IGNORE_ENT_ARG);
      Set<EntityType> ignoreTypes = new ObjectOpenHashSet<>(list);

      entityFilter = entityFilter.and(entity -> {
        return !ignoreTypes.contains(entity.getType());
      });
    }

    StructureFillConfig config = StructureFillConfig.builder()
        .area(selection)
        .blockPredicate(blockFilter)
        .entityPredicate(entityFilter)
        .includeFunctionBlocks(args.has(INCLUDE_FUNCTIONS))
        .paletteName(paletteName)
        .build();

    structure.fill(config);

    switch (fillType) {
      case ft_redefine -> {
        registry.register(registryKey, structure);

        source.sendSuccess(
            Messages.render("structures.redefined")
                .addValue("structure", registryKey)
                .create(source)
        );
      }

      case ft_palette_add -> {
        source.sendSuccess(
            Messages.render("structures.palettes.added")
                .addValue("name", paletteName)
                .addValue("structure", registryKey)
                .create(source)
        );
      }

      case ft_create -> {
        registry.register(registryKey, structure);

        source.sendSuccess(
            Messages.render("structures.created")
                .addValue("name", registryKey)
                .create(source)
        );
      }

      default -> throw new IllegalStateException("Unexpected value: " + fillType);
    }

    return SINGLE_SUCCESS;
  }

  private int placeStructure(CommandContext<CommandSource> c) throws CommandSyntaxException {
    Holder<BlockStructure> holder = getStructure(c);
    Builder cfgBuilder = StructurePlaceConfig.builder();

    if (CommandContexts.getArguments(c).containsKey("options")) {
      ParsedOptions options = ArgumentTypes.getOptions(c, "options");
      StructureCommands.configurePlacement(cfgBuilder, c.getSource(), options, PALETTE_NAME);
    } else {
      cfgBuilder
          .pos(Vectors.intFrom(c.getSource().getLocation()))
          .world(c.getSource().getWorld());
    }

    StructurePlaceConfig build = cfgBuilder.build();
    holder.getValue().place(build);

    c.getSource().sendSuccess(
        Messages.render("structures.placed")
            .create(c.getSource())
    );
    return SINGLE_SUCCESS;
  }

  private RequiredArgumentBuilder<CommandSource, Holder<BlockStructure>> structureArg() {
    return argument(STRUCTURE_ARG, argumentType);
  }

  private Holder<BlockStructure> getStructure(CommandContext<CommandSource> c) {
    return c.getArgument(STRUCTURE_ARG, Holder.class);
  }

  private RequiredArgumentBuilder<CommandSource, Holder<BlockStructure>> createDataArgument() {
    RequiredArgumentBuilder<CommandSource, Holder<BlockStructure>> arg = structureArg();
    DataCommands.addArguments(arg, "Structure", HEADER_ACCESSOR);
    return arg;
  }
}
