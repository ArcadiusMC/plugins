package net.arcadiusmc.usables.commands;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.ComponentList;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.objects.Usable;
import net.arcadiusmc.usables.objects.UsableObject;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class InteractableCommand<H extends Usable> extends UsableCommand<H> {

  public InteractableCommand(String name, String argumentName) {
    super(name, argumentName);
  }

  @Override
  protected void createUsages(UsageFactory factory) {
    super.createUsages(factory);

    factory.usage("silent", "Checks if a usable is silent");
    factory.usage("silent <true | false>", "Sets a usable to be silent or not");

    UsablesCommands.actions.createUsages(factory);
    UsablesCommands.conditions.createUsages(factory);
  }

  @Override
  protected <B extends ArgumentBuilder<CommandSource, B>> void createEditArguments(
      B argument,
      UsableProvider<H> provider
  ) {
    super.createEditArguments(argument, provider);

    argument.then(literal("silent")
        .requires(hasAdminPermission())

        .executes(c -> {
          H holder = provider.get(c);

          c.getSource().sendMessage(
              Text.format("&e{0}&r will send fail messages: &f{1}&r.",
                  NamedTextColor.GRAY,
                  holder.displayName(),
                  !holder.isSilent()
              )
          );
          return 0;
        })

        .then(argument("state", BoolArgumentType.bool())
            .executes(c -> {
              H holder = provider.get(c);
              boolean silent = c.getArgument("state", Boolean.class);

              holder.setSilent(silent);
              provider.postEdit(holder);

              c.getSource().sendSuccess(
                  Text.format("&e{0}&r will now failure messages: &f{1}&r.",
                      NamedTextColor.GRAY,
                      holder.displayName(),
                      !holder.isSilent()
                  )
              );
              return 0;
            })
        )
    );

    ListAccess<Condition> listAccess = context -> {
      H holder = provider.get(context);
      return new ConditionListAccess<>(holder, provider);
    };

    argument.then(errorOverrideArguments(provider, listAccess));

    argument.then(UsablesCommands.actions.create(context -> {
      H holder = provider.get(context);
      return new ActionListAccess<>(holder, provider);
    }).requires(hasAdminPermission()));

    argument.then(UsablesCommands.conditions.create(listAccess).requires(hasAdminPermission()));
  }


  private LiteralArgumentBuilder<CommandSource> errorOverrideArguments(
      UsableProvider<H> provider,
      ListAccess<Condition> listAccess
  ) {
    return literal("error-overrides")
        .then(literal("list")
            .executes(c -> {
              H holder = provider.get(c);
              String[] overrides = holder.getErrorOverrides();
              ComponentList<Condition> conditions = holder.getConditions();

              if (overrides == null || overrides.length < 1) {
                throw Exceptions.NOTHING_TO_LIST.exception(c.getSource());
              }

              TextWriter writer = TextWriters.newWriter();

              for (int i = 0; i < conditions.size(); i++) {
                ObjectType<Condition> type = conditions.getType(i);
                String override;

                if (i >= overrides.length) {
                  override = "";
                } else {
                  String str = overrides[i];

                  if (Strings.isNullOrEmpty(str)) {
                    override = "";
                  } else {
                    override = str;
                  }
                }

                writer.formattedLine("{0, number}) {1} {2}",
                    i + 1,
                    conditions.displayType(type),
                    override
                );
              }

              c.getSource().sendMessage(writer.asComponent());
              return 0;
            })
        )

        .then(literal("global")
            .executes(c -> {
              H holder = provider.get(c);
              String globalMessage = holder.getGlobalErrorOverride();

              if (Strings.isNullOrEmpty(globalMessage)) {
                throw Exceptions.format("No global error message set for {0}",
                    holder.displayName()
                );
              }

              c.getSource().sendMessage(
                  Text.format("{0} global error message: '&f{1}&r'",
                      NamedTextColor.GRAY,
                      globalMessage
                  )
              );

              return 0;
            })

            .then(literal("unset")
                .executes(c -> setGlobalOverride(c, provider, false))
            )

            .then(literal("set").then(argument("text", StringArgumentType.greedyString())
                .suggests(Arguments.CHAT::listSuggestions)
                .executes(c -> setGlobalOverride(c, provider, true))
            ))
        )

        .then(literal("set")
            .then(argument("index", IntegerArgumentType.integer(1))
                .suggests(UsablesCommands.conditions.suggestIndices(listAccess))

                .then(argument("text", StringArgumentType.greedyString())
                    .suggests(Arguments.CHAT::listSuggestions)
                    .executes(c -> setErrorOverride(c, provider))
                )
            )
        )

        .then(literal("clear")
            .executes(c -> {
              H holder = provider.get(c);
              holder.setErrorOverrides(null);
              provider.postEdit(holder);

              c.getSource().sendSuccess(
                  Component.text("Cleared error-override messages", NamedTextColor.GRAY)
              );

              return 0;
            })
        )

        .then(literal("remove")
            .then(argument("index", IntegerArgumentType.integer(1))
                .suggests(UsablesCommands.conditions.suggestIndices(listAccess))
                .executes(c -> removeErrorOverride(c, provider))
            )
        );
  }

  private int removeErrorOverride(
      CommandContext<CommandSource> c,
      UsableProvider<H> provider
  ) throws CommandSyntaxException {
    H holder = provider.get(c);
    int index = c.getArgument("index", Integer.class);

    String[] overrides = holder.getErrorOverrides();
    ComponentList<Condition> conditions = holder.getConditions();
    int size = conditions.size();

    Commands.ensureIndexValid(index, size);

    String existing = index >= overrides.length
        ? null
        : overrides[index - 1];

    if (Strings.isNullOrEmpty(existing)) {
      throw Exceptions.create("Nothing changed");
    }

    overrides[index - 1] = null;
    holder.setErrorOverrides(overrides);
    provider.postEdit(holder);

    c.getSource().sendSuccess(
        Text.format("Removed {0} error message at index {1, number}",
            NamedTextColor.GRAY,
            holder.displayName(),
            index
        )
    );

    return 0;
  }

  private int setGlobalOverride(
      CommandContext<CommandSource> c,
      UsableProvider<H> provider,
      boolean valueSet
  ) throws CommandSyntaxException {
    String value;

    if (valueSet) {
      value = c.getArgument("text", String.class);
    } else {
      value = null;
    }

    H holder = provider.get(c);
    holder.setGlobalErrorOverride(value);
    provider.postEdit(holder);

    CommandSource source = c.getSource();

    if (valueSet) {
      source.sendSuccess(
          Text.format("Set {0} global error message to '&f{1}&r'",
              NamedTextColor.GRAY,
              holder.displayName(),
              value
          )
      );
    } else {
      source.sendSuccess(
          Text.format("Removed {0} global error message",
              NamedTextColor.GRAY,
              holder.displayName()
          )
      );
    }

    return 0;
  }

  private int setErrorOverride(CommandContext<CommandSource> c, UsableProvider<H> provider)
      throws CommandSyntaxException
  {
    H holder = provider.get(c);

    int index = c.getArgument("index", Integer.class);
    String text = c.getArgument("text", String.class);

    String[] overrides = holder.getErrorOverrides();
    ComponentList<Condition> conditions = holder.getConditions();

    Commands.ensureIndexValid(index, conditions.size());

    String[] correctSize;

    if (overrides == null || overrides.length == 0) {
      correctSize = new String[conditions.size()];
    } else {
      correctSize = ObjectArrays.ensureCapacity(overrides, index + 1);
    }

    correctSize[index - 1] = text;

    holder.setErrorOverrides(correctSize);
    provider.postEdit(holder);

    c.getSource().sendMessage(
        Text.format("Set {0} error message of condition at index {1, number} to '&f{2}&r'",
            NamedTextColor.GRAY,
            holder.displayName(),
            index,
            text
        )
    );

    return 0;
  }

  record ActionListAccess<T extends Usable>(
      T usable,
      UsableProvider<T> provider
  ) implements ListHolder<Action> {

    @Override
    public ComponentList<Action> getList() {
      return usable.getActions();
    }

    @Override
    public void postEdit() {
      provider.postEdit(usable);
    }

    @Override
    public UsableObject object() {
      return usable;
    }
  }

  record ConditionListAccess<T extends Usable>(
      T usable,
      UsableProvider<T> provider
  ) implements ListHolder<Condition> {

    @Override
    public ComponentList<Condition> getList() {
      return usable.getConditions();
    }

    @Override
    public void postEdit() {
      provider.postEdit(usable);
    }

    @Override
    public UsableObject object() {
      return usable;
    }

    @Override
    public void onRemove(IntRange removeRange) {
      ComponentList<Condition> conditions = getList();
      String[] errorOverrides = usable().getErrorOverrides();

      if (errorOverrides == null || errorOverrides.length < 1) {
        return;
      }

      int min = removeRange.min().orElse(0);
      int max = removeRange.max().orElse(conditions.size());

      if (min == 0 && max >= conditions.size()) {
        usable.setErrorOverrides(null);
        return;
      }

      for (int i = min; i < max; i++) {
        if (errorOverrides.length <= i) {
          continue;
        }

        errorOverrides[i] = null;
        continue;
      }

    }

    @Override
    public void onClear() {
      usable.setErrorOverrides(null);
    }
  }
}
