package net.arcadiusmc.structure.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.arcadiusmc.structure.BlockStructure.DEFAULT_PALETTE_NAME;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collections;
import java.util.Date;
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
import net.arcadiusmc.structure.FillResult;
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
import net.forthecrown.grenadier.types.BlockFilterArgument.Result;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.FlagOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
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

  private static final FlagOption USE_GMASK
      = Options.flag("use-gmask");

  private static final OptionsArgument FILL_ARGS = OptionsArgument.builder()
      .addOptional(BLOCK_FILTER)
      .addOptional(IGNORE_ENT_ARG)
      .addFlag(INCLUDE_FUNCTIONS)
      .addFlag(USE_GMASK)
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

    ParsedOptions args;
    String optionsInput;

    if (arguments.containsKey("options")) {
      args = ArgumentTypes.getOptions(c, "options");
      optionsInput = CommandContexts.getInput(c, "options");
    } else {
      args = ParsedOptions.EMPTY;
      optionsInput = null;
    }

    Predicate<Block> blockFilter = createBlockFilter(source, args);
    Predicate<Entity> entityFilter = createEntityFilter(args);

    StructureFillConfig config = StructureFillConfig.builder()
        .area(selection)
        .blockPredicate(blockFilter)
        .entityPredicate(entityFilter)
        .includeFunctionBlocks(args.has(INCLUDE_FUNCTIONS))
        .paletteName(paletteName)
        .build();

    FillResult result = structure.fill(config);
    String messageKey;

    switch (fillType) {
      case ft_redefine -> {
        registry.register(registryKey, structure);
        messageKey = "structures.redefined";
      }

      case ft_palette_add -> {
        messageKey = "structures.palettes.added";
      }

      case ft_create -> {
        registry.register(registryKey, structure);
        messageKey = "structures.created";
      }

      default -> throw new IllegalStateException("Unexpected value: " + fillType);
    }

    recordCopySource(selection, structure, source, paletteName, optionsInput);

    Component baseMessage = Messages.render(messageKey)
        .addValue("structure", registryKey)
        .addValue("palette", paletteName)
        .create(source);

    if (!source.acceptsSuccessMessage() || source.isSilent()) {
      return SINGLE_SUCCESS;
    }

    source.broadcastAdmin(baseMessage);

    source.sendMessage(
        Messages.render("structures.scanTemplate")
            .addValue("baseMessage", baseMessage)

            .addValue("includeFunctions", args.has(INCLUDE_FUNCTIONS))
            .addValue("blockFilter", args.has(BLOCK_FILTER))
            .addValue("entityFilter", args.has(IGNORE_ENT_ARG))
            .addValue("gmask", args.has(USE_GMASK))

            .addValue("size", result.getSize())
            .addValue("blockCount", result.getBlockCount())
            .addValue("functionCount", result.getFunctionCount())
            .addValue("entityCount", result.getEntityCount())

            .create(source)
    );

    return SINGLE_SUCCESS;
  }

  private void recordCopySource(
      AreaSelection selection,
      BlockStructure structure,
      CommandSource source,
      String paletteName,
      String optionsInput
  ) {
    Vector3i copyMin = selection.min();
    Vector3i copyMax = selection.max();

    CompoundTag copySourceRecord = BinaryTags.compoundTag();
    copySourceRecord.putString("date", new Date().toString());
    copySourceRecord.putString("world_name", selection.getWorld().getName());
    copySourceRecord.putString("author", source.textName());
    copySourceRecord.put("min_point", Vectors.writeTag(copyMin));
    copySourceRecord.put("max_point", Vectors.writeTag(copyMax));

    if (optionsInput != null) {
      copySourceRecord.putString("scan_options", optionsInput);
    }

    CompoundTag header = structure.getHeader();
    CompoundTag paletteSources = header.getCompound("copy_sources");

    paletteSources.put(paletteName, copySourceRecord);
    header.put("copy_sources", paletteSources);
  }

  private Predicate<Entity> createEntityFilter(ParsedOptions args) {
    Predicate<Entity> entityFilter = entity -> entity.getType() != EntityType.PLAYER;

    if (!args.has(IGNORE_ENT_ARG)) {
      return entityFilter;
    }

    List<EntityType> list = args.getValue(IGNORE_ENT_ARG);
    Set<EntityType> ignoreTypes = new ObjectOpenHashSet<>(list);

    entityFilter = entityFilter.and(entity -> {
      return !ignoreTypes.contains(entity.getType());
    });

    return entityFilter;
  }

  private Predicate<Block> createBlockFilter(CommandSource source, ParsedOptions args)
      throws CommandSyntaxException
  {
    Player player = source.asPlayer();
    Predicate<Block> blockFilter = block -> block.getType() != Material.STRUCTURE_VOID;

    if (args.has(BLOCK_FILTER)) {
      List<Result> list = args.getValue(BLOCK_FILTER);
      assert list != null;

      blockFilter = blockFilter.and(block -> {
        for (Result r : list) {
          if (!r.test(block)) {
            continue;
          }

          return false;
        }

        return true;
      });
    }

    if (args.has(USE_GMASK)) {
      BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
      Mask gmask = wePlayer.getSession().getMask();

      if (gmask == null) {
        throw Messages.render("structures.errors.noGmask")
            .exception(source);
      }

      blockFilter = blockFilter.and(block -> {
        return gmask.test(BlockVector3.at(block.getX(), block.getY(), block.getZ()));
      });
    }

    return blockFilter;
  }

  private int placeStructure(CommandContext<CommandSource> c) throws CommandSyntaxException {
    Holder<BlockStructure> holder = getStructure(c);
    Builder cfgBuilder = StructurePlaceConfig.builder();
    CommandSource source = c.getSource();

    if (CommandContexts.getArguments(c).containsKey("options")) {
      ParsedOptions options = ArgumentTypes.getOptions(c, "options");
      StructureCommands.configurePlacement(cfgBuilder, source, options, PALETTE_NAME);
    } else {
      cfgBuilder
          .pos(Vectors.intFrom(source.getLocation()))
          .world(source.getWorld());
    }

    StructurePlaceConfig build = cfgBuilder.build();
    holder.getValue().place(build);

    Component base = Messages.render("structures.placed")
        .addValue("structure", holder.getKey())
        .create(source);

    if (!source.isSilent() && source.acceptsSuccessMessage()) {
      source.broadcastAdmin(base);
    }

    source.sendMessage(
        Messages.render("structures.placementTemplate")
            .addValue("base", base)

            .addValue("structure", holder.getKey())
            .addValue("palette", cfgBuilder.paletteName())

            .addValue("position", cfgBuilder.pos())
            .addValue("entities", cfgBuilder.entitySpawner() != null)

            .addValue("offset", cfgBuilder.transform().getOffset())
            .addValue("pivot", cfgBuilder.transform().getPivot())
            .addValue("rotation", cfgBuilder.transform().getRotation())

            .create(source)
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
