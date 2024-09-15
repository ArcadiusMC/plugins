package net.arcadiusmc.usables.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.UPermissions;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.trigger.AreaTrigger;
import net.arcadiusmc.usables.trigger.AreaTrigger.Type;
import net.arcadiusmc.usables.trigger.TriggerDraw;
import net.arcadiusmc.usables.trigger.TriggerManager;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.spongepowered.math.vector.Vector3i;

public class UsableTriggerCommand extends InteractableCommand<AreaTrigger> {

  private final TriggerManager manager;

  public UsableTriggerCommand(TriggerManager manager) {
    super("usabletriggers");
    setAliases("usable_triggers", "triggers");
    setPermission(UPermissions.TRIGGER);

    this.manager = manager;
  }

  @Override
  public String getAdminPermission() {
    return UPermissions.TRIGGER.getName();
  }

  @Override
  protected String usagePrefix() {
    return "<trigger>";
  }

  @Override
  protected void createUsages(UsageFactory factory) {
    factory.usage("rename <trigger name> <new name>")
        .addInfo("Renames a trigger to <name>");

    factory.usage("redefine <trigger name>")
        .addInfo("Redefines a trigger to your current world edit selection");

    factory.usage("select")
        .addInfo("Makes a trigger become your WorldEdit selection.");

    factory.usage("remove").addInfo("Removes a trigger");

    super.createUsages(factory);
  }

  @Override
  protected void createPrefixedUsages(UsageFactory factory) {
    factory.usage("toggle-rendering")
        .addInfo("Toggles/disabled nearby triggers being drawn.")
        .addInfo("Each particle is color coded by type:")
        .addInfo("- red: exit-type triggers")
        .addInfo("- green: enter-type triggers")
        .addInfo("- blue: move-type triggers")
        .addInfo("- red-green: exit-or-enter-type triggers");

    factory.usage("create <name>")
        .addInfo("Defines a new trigger, using your world edit")
        .addInfo("selection as the trigger's area");
  }

  @Override
  protected void addPrefixedArguments(LiteralArgumentBuilder<CommandSource> builder) {
    builder
        .then(literal("toggle-rendering")
            .executes(c -> {
              org.bukkit.entity.Player player = c.getSource().asPlayer();
              TriggerDraw draw = manager.getDraw();

              if (draw.isToggled(player)) {
                draw.removePlayer(player);
                player.sendMessage(Component.text("Stopped drawing triggers", NamedTextColor.GRAY));
              } else {
                draw.addPlayer(player);
                player.sendMessage(Component.text("Started drawing triggers", NamedTextColor.YELLOW));
              }

              return SINGLE_SUCCESS;
            })
        )

        .then(literal("define")
            .then(argument("name", Arguments.RESOURCE_KEY)
                .executes(c -> {
                  define(c, false);
                  return 0;
                })
                // Area input given, use that
                .then(argument("pos1", ArgumentTypes.blockPosition())
                    .then(argument("pos2", ArgumentTypes.blockPosition())
                        .executes(c -> {
                          define(c, true);
                          return 0;
                        })
                    )
                )
            )
        );
  }

  private void define(CommandContext<CommandSource> c, boolean boundsSet)
      throws CommandSyntaxException
  {
    String name = c.getArgument("name", String.class);
    AreaTrigger existing = manager.get(name);

    if (existing != null) {
      throw alreadyExists(existing.displayName());
    }

    WorldBounds3i area;

    if (boundsSet) {
      Location p1 = ArgumentTypes.getLocation(c, "pos1");
      Location p2 = ArgumentTypes.getLocation(c, "pos2");
      area = WorldBounds3i.of(p1, p2);
    } else {
      area = WorldBounds3i.ofPlayerSelection(c.getSource().asPlayer());

      if (area == null) {
        throw Exceptions.NO_REGION_SELECTION;
      }
    }

    AreaTrigger trigger = new AreaTrigger();
    trigger.setName(name);
    trigger.setArea(area);

    manager.add(trigger);

    c.getSource().sendSuccess(
        Text.format("Created trigger called '&e{0}&r'", NamedTextColor.GRAY, trigger.displayName())
    );
  }

  static CommandSyntaxException alreadyExists(Object displayName) {
    return Exceptions.format("Trigger named '{0}' already exists", displayName);
  }

  @Override
  protected <B extends ArgumentBuilder<CommandSource, B>> void createEditArguments(
      B argument,
      UsableProvider<AreaTrigger> provider
  ) {
    super.createEditArguments(argument, provider);

    argument.then(literal("remove").executes(c -> {
      AreaTrigger trigger = provider.get(c);
      manager.remove(trigger);

      c.getSource().sendSuccess(
          Text.format("Removed trigger &e{0}&r.", NamedTextColor.GRAY, trigger.displayName())
      );
      return 0;
    }));

    argument.then(literal("redefine")
        .executes(c -> {
          AreaTrigger trigger = provider.get(c);
          var newArea = WorldBounds3i.ofPlayerSelection(c.getSource().asPlayer());

          if (newArea == null) {
            throw Exceptions.NO_REGION_SELECTION;
          }

          trigger.setArea(newArea);

          c.getSource().sendSuccess(
              Text.format("Redefined trigger &e{0}&r to area &6{1}&r.",
                  NamedTextColor.GRAY,
                  trigger.displayName(), newArea
              )
          );
          return 0;
        })
    );

    argument.then(literal("select")
        .executes(c -> {
          AreaTrigger trigger = provider.get(c);

          Vector3i min = trigger.getArea().min();
          Vector3i max = trigger.getArea().max();

          CuboidRegionSelector region = new CuboidRegionSelector(
              BukkitAdapter.adapt(trigger.getArea().getWorld()),
              BlockVector3.at(min.x(), min.y(), min.z()),
              BlockVector3.at(max.x(), max.y(), max.z())
          );

          Player wePlayer = BukkitAdapter.adapt(c.getSource().asPlayer());
          LocalSession session = wePlayer.getSession();

          session.setRegionSelector(region.getWorld(), region);
          region.explainRegionAdjust(wePlayer, session);

          c.getSource().sendMessage(Component.text("Region selected!", NamedTextColor.GRAY));
          return 0;
        })
    );

    argument.then(literal("rename")
        .then(argument("newName", Arguments.RESOURCE_KEY)
            .suggests((context, builder) -> {
              AreaTrigger trigger = provider.get(context);
              return Completions.suggest(builder, trigger.getName());
            })
            .executes(c -> {
              AreaTrigger trigger = provider.get(c);

              var oldName = trigger.getName();
              String name = c.getArgument("newName", String.class);

              var existing = manager.get(name);
              if (existing != null) {
                throw alreadyExists(trigger.displayName());
              }

              trigger.setName(name);

              c.getSource().sendSuccess(
                  Text.format("Renamed trigger '&e{0}&r' to '&6{1}&r'",
                      NamedTextColor.GRAY,
                      oldName, name
                  )
              );
              return 0;
            })
        )
    );

    argument.then(literal("type")
        .executes(c -> {
          var trigger = provider.get(c);

          c.getSource().sendMessage(
              Text.format("&e{0}&r's type is '&6{1}&r'",
                  trigger.displayName(),
                  trigger.getType().name().toLowerCase()
              )
          );
          return 0;
        })

        .then(argument("type", ArgumentTypes.enumType(Type.class))
            .executes(c -> {
              var trigger = provider.get(c);
              var type = c.getArgument("type", Type.class);

              trigger.setType(type);

              c.getSource().sendSuccess(
                  Text.format("Set &e{0}&r's type to &6{1}&r.",
                      NamedTextColor.GRAY,
                      trigger.displayName(),
                      type.name().toLowerCase()
                  )
              );
              return 0;
            })
        )
    );
  }

  @Override
  protected ArgumentType<?> getArgumentType() {
    return new TriggerArgumentType(manager);
  }

  @Override
  protected UsableProvider<AreaTrigger> getProvider(String argument) {
    return context -> context.getArgument(argument, AreaTrigger.class);
  }

  public static class TriggerArgumentType implements ArgumentType<AreaTrigger> {

    private final TriggerManager manager;

    public TriggerArgumentType(TriggerManager manager) {
      this.manager = manager;
    }

    @Override
    public AreaTrigger parse(StringReader reader) throws CommandSyntaxException {
      final int start = reader.getCursor();

      String ftcKey = Arguments.RESOURCE_KEY.parse(reader);

      var triggers = UsablesPlugin.get().getTriggers();
      AreaTrigger trigger = triggers.get(ftcKey);

      if (trigger == null) {
        reader.setCursor(start);
        throw Exceptions.unknown("Trigger", reader, ftcKey);
      }

      return trigger;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
      return Completions.suggest(builder, manager.getNames());
    }
  }
}
