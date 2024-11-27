package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.arcadiusmc.text.Text.displayTag;
import static net.arcadiusmc.text.Text.format;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.utils.VanillaAccess;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.EntitySelector;
import net.forthecrown.grenadier.types.ParsedPosition;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.paper.PaperNbt;
import net.forthecrown.nbt.path.TagPath;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CommandDataGet extends BaseCommand {

  public CommandDataGet() {
    super("dataget");
    setDescription("Better version of Mojang's '/data get' command");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(create("entity", DataProvider.ENTITY));
    command.then(create("block", DataProvider.BLOCK));
    command.then(create("storage", DataProvider.STORAGE));
  }

  private <A, T> LiteralArgumentBuilder<CommandSource> create(
      String name,
      DataProvider<A, T> provider
  ) {
    var literal = literal(name);

    if (provider.supportsTargeted()) {
      literal.executes(facingCommand(provider, false));

      literal.then(
          literal("facing")
              .executes(facingCommand(provider, false))

              .then(argument("path", ArgumentTypes.tagPath())
                  .executes(facingCommand(provider, true))
              )
      );
    }

    literal
        .then(argument("value", provider.argumentType())
            .executes(c -> {
              A a = (A) c.getArgument("value", Object.class);
              return display(c.getSource(), a, provider, null);
            })

            .then(argument("path", ArgumentTypes.tagPath())
                .executes(c -> {
                  A a = (A) c.getArgument("value", Object.class);
                  TagPath path = c.getArgument("path", TagPath.class);

                  return display(c.getSource(), a, provider, path);
                })
            )
        );

    return literal;
  }

  private <A, T> Command<CommandSource> facingCommand(
      DataProvider<A, T> provider,
      boolean pathGiven
  ) {
    return context -> {
      Player player = context.getSource().asPlayer();

      T facing = provider.getFacing(player);
      BinaryTag data = provider.getData(facing);

      TagPath path;

      if (pathGiven) {
        path = context.getArgument("path", TagPath.class);
      } else {
        path = null;
      }

      List<BinaryTag> list = processPath(data, path);

      for (BinaryTag binaryTag : list) {
        Component text = prettyPrint(provider, facing, path, binaryTag);
        context.getSource().sendMessage(text);
      }

      return SINGLE_SUCCESS;
    };
  }

  private <A, T> int display(CommandSource source, A a, DataProvider<A, T> provider, TagPath path)
      throws CommandSyntaxException
  {
    T t = provider.get(a, source);
    BinaryTag data = provider.getData(t);
    List<BinaryTag> tags = processPath(data, path);

    for (BinaryTag tag : tags) {
      Component text = prettyPrint(provider, t, path, tag);
      source.sendMessage(text);
    }

    return SINGLE_SUCCESS;
  }

  private <A, T> Component prettyPrint(
      DataProvider<A, T> provider,
      T t,
      TagPath path,
      BinaryTag tag
  ) {
    Component dataText = displayTag(tag, true);
    Component displayName = provider.displayName(t);

    if (path == null) {
      return format("&e{0}&7 data: &f{1}", displayName, dataText);
    }

    return format("&e{0}&7 data at {1}: &f{2}", displayName, path.getInput(), dataText);
  }

  private List<BinaryTag> processPath(BinaryTag tag, TagPath path) throws CommandSyntaxException {
    if (path == null) {
      return List.of(tag);
    }

    List<BinaryTag> list = path.get(tag);

    if (list.isEmpty()) {
      throw Exceptions.format("No data at path {0}", path.getInput());
    }

    return list;
  }

  interface DataProvider<A, T> {

    DataProvider<EntitySelector, Entity> ENTITY = new DataProvider<EntitySelector, Entity>() {
      @Override
      public ArgumentType<EntitySelector> argumentType() {
        return ArgumentTypes.entity();
      }

      @Override
      public Entity get(EntitySelector entitySelector, CommandSource source)
          throws CommandSyntaxException
      {
        return entitySelector.findEntity(source);
      }

      @Override
      public Entity getFacing(Player player) throws CommandSyntaxException {
        Entity target =  player.getTargetEntity(5);

        if (target == null) {
          throw Exceptions.create("Not looking at any entity");
        }

        return target;
      }

      @Override
      public boolean supportsTargeted() {
        return true;
      }

      @Override
      public BinaryTag getData(Entity entity) {
        return PaperNbt.saveEntity(entity);
      }

      @Override
      public Component displayName(Entity entity) {
        return entity.teamDisplayName();
      }
    };

    DataProvider<ParsedPosition, TileState> BLOCK = new DataProvider<>() {
      @Override
      public ArgumentType<ParsedPosition> argumentType() {
        return ArgumentTypes.blockPosition();
      }

      @Override
      public TileState get(ParsedPosition parsedPosition, CommandSource source)
          throws CommandSyntaxException
      {
        return fromBlock(parsedPosition.apply(source).getBlock());
      }

      @Override
      public TileState getFacing(Player player) throws CommandSyntaxException {
        Block facing = player.getTargetBlockExact(5);
        if (facing == null) {
          throw Exceptions.create("Not looking at a block");
        }
        return fromBlock(facing);
      }

      private TileState fromBlock(Block block) throws CommandSyntaxException {
        BlockState state = block.getState();

        if (!(state instanceof TileState tile)) {
          throw Exceptions.create("Not a block entity");
        }

        return tile;
      }

      @Override
      public boolean supportsTargeted() {
        return true;
      }

      @Override
      public BinaryTag getData(TileState block) {
        return PaperNbt.saveBlockEntity(block);
      }

      @Override
      public Component displayName(TileState tileState) {
        return format("{0} {1} {2}", tileState.getX(), tileState.getY(), tileState.getZ());
      }
    };

    DataProvider<NamespacedKey, NamespacedKey> STORAGE = new DataProvider<>() {
      @Override
      public ArgumentType<NamespacedKey> argumentType() {
        return ArgumentTypes.key();
      }

      @Override
      public NamespacedKey get(NamespacedKey namespacedKey, CommandSource source) {
        return namespacedKey;
      }

      @Override
      public NamespacedKey getFacing(Player player) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean supportsTargeted() {
        return false;
      }

      @Override
      public BinaryTag getData(NamespacedKey namespacedKey) {
        return VanillaAccess.getStoredData(namespacedKey);
      }

      @Override
      public Component displayName(NamespacedKey namespacedKey) {
        return Component.text(namespacedKey.toString());
      }
    };

    ArgumentType<A> argumentType();

    T get(A a, CommandSource source) throws CommandSyntaxException;

    T getFacing(Player player) throws CommandSyntaxException;

    boolean supportsTargeted();

    BinaryTag getData(T t);

    Component displayName(T t);
  }
}
