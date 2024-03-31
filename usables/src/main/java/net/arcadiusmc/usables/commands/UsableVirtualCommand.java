package net.arcadiusmc.usables.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.list.ComponentList;
import net.arcadiusmc.usables.objects.UsableObject;
import net.arcadiusmc.usables.virtual.Trigger;
import net.arcadiusmc.usables.virtual.VirtualUsable;
import net.arcadiusmc.usables.virtual.VirtualUsableManager;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.kyori.adventure.text.format.NamedTextColor;

public class UsableVirtualCommand extends InteractableCommand<VirtualUsable> {

  final VirtualUsableManager manager;
  final ListCommands<Trigger> triggers;

  public UsableVirtualCommand() {
    super("virtual-usable");
    setAliases("vusable", "virtualusable");

    this.manager = UsablesPlugin.get().getVirtuals();
    this.triggers = new ListCommands<>("triggers", "Trigger", manager.getTriggerTypes());
  }

  @Override
  protected void createUsages(UsageFactory factory) {
    factory.usage("rename").addInfo("Renames a virtual usable");
    super.createUsages(factory);
  }

  @Override
  protected void createPrefixedUsages(UsageFactory factory) {
    factory.usage("create <name>").addInfo("Creates a virtual usable.");
  }

  @Override
  protected void addPrefixedArguments(LiteralArgumentBuilder<CommandSource> builder) {
    builder
        .then(literal("create")
            .then(argument("name", Arguments.RESOURCE_KEY)
                .executes(c -> {
                  String name = c.getArgument("name", String.class);
                  VirtualUsable usable = manager.getUsable(name);

                  if (usable != null) {
                    throw Exceptions.format("Virtual usable named '{0}' already exists", name);
                  }

                  usable = new VirtualUsable(name);
                  manager.add(usable);

                  c.getSource().sendSuccess(
                      Text.format("Created virtual usable '&f{0}&r'",
                          NamedTextColor.GRAY,
                          name
                      )
                  );

                  return 0;
                })
            )
        );
  }

  @Override
  protected <B extends ArgumentBuilder<CommandSource, B>> void createEditArguments(
      B argument,
      UsableProvider<VirtualUsable> provider
  ) {
    super.createEditArguments(argument, provider);
    argument.then(vanillaCancelArguments(provider));

    argument
        .then(literal("rename")
            .then(argument("new-name", Arguments.RESOURCE_KEY)
                .executes(c -> {
                  VirtualUsable usable = provider.get(c);
                  String name = c.getArgument("new-name", String.class);

                  String prevName = usable.getName();
                  usable.setName(name);

                  c.getSource().sendSuccess(
                      Text.format("Renamed virtual usable '&f{0}&r' to '&f{1}&r'",
                          NamedTextColor.GRAY,
                          prevName, name
                      )
                  );

                  return 0;
                })
            )
        );

    argument
        .then(triggers.create(context -> {
          VirtualUsable usable = provider.get(context);
          return new VirtualListHolder(usable);
        }));
  }

  @Override
  protected ArgumentType<?> getArgumentType() {
    return VirtualArgument.ARGUMENT;
  }

  @Override
  protected UsableProvider<VirtualUsable> getProvider(String argument) {
    return context -> context.getArgument(argument, VirtualUsable.class);
  }

  record VirtualListHolder(VirtualUsable usable) implements ListHolder<Trigger> {

    @Override
    public ComponentList<Trigger> getList() {
      return usable.getTriggers();
    }

    @Override
    public void postEdit() {

    }

    @Override
    public UsableObject object() {
      return usable;
    }
  }

  enum VirtualArgument implements ArgumentType<VirtualUsable> {
    ARGUMENT;

    @Override
    public VirtualUsable parse(StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();

      String ftcKey = Arguments.RESOURCE_KEY.parse(reader);
      VirtualUsableManager manager = UsablesPlugin.get().getVirtuals();

      VirtualUsable usable = manager.getUsable(ftcKey);
      if (usable == null) {
        reader.setCursor(start);
        throw Exceptions.formatWithContext("Unknown virtual usable: '{0}'", reader, ftcKey);
      }

      return usable;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
      VirtualUsableManager manager = UsablesPlugin.get().getVirtuals();
      return Completions.suggest(builder, manager.getNames());
    }
  }
}
