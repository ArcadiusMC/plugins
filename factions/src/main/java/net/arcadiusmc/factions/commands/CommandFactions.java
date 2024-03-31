package net.arcadiusmc.factions.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.factions.FExceptions;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionMember;
import net.arcadiusmc.factions.FactionProperty;
import net.arcadiusmc.factions.FactionsPlugin;
import net.arcadiusmc.factions.Properties;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandContexts;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

@CommandFile("commands/factions.gcn")
public class CommandFactions {

  static final String F_ARG = "faction";

  private final FactionsPlugin plugin;

  CommandFactions(FactionsPlugin plugin) {
    this.plugin = plugin;
  }

  @VariableInitializer
  void initVars(Map<String, Object> vars) {
    vars.put("faction", FactionArgument.FACTION);
    vars.put("f_property", new RegistryArguments<>(Properties.REGISTRY, "Faction Property"));
  }

  void reloadConfig(CommandSource source) {
    plugin.reloadConfig();
    source.sendSuccess(Messages.renderText("factions.cmd.reloaded.config", source));
  }

  void reloadFactions(CommandSource source) {
    plugin.getManager().load();
    source.sendSuccess(Messages.renderText("factions.cmd.reloaded.plugin", source));
  }

  void savePlugin(CommandSource source) {
    plugin.save();
    source.sendSuccess(Messages.renderText("factions.cmd.saved", source));
  }

  void listFactions(CommandSource source) throws CommandSyntaxException {
    Collection<Faction> factions = plugin.getManager().getFactions();

    if (factions.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    TextJoiner joiner = TextJoiner.onComma()
        .add(factions.stream().map(faction -> faction.displayName(source)));

    source.sendMessage(
        Messages.render("factions.cmd.list.format")
            .addValue("list", joiner.asComponent())
            .create(source)
    );
  }

  void createFaction(CommandSource source, @Argument("id") String key)
      throws CommandSyntaxException
  {
    Faction existing = plugin.getManager().getFaction(key);

    if (existing != null) {
      throw Messages.render("factions.errors.alreadyExists")
          .addValue("name", existing.displayName(source))
          .exception(source);
    }

    Faction created = new Faction(key);
    plugin.getManager().addFaction(created);

    source.sendSuccess(
        Messages.render("factions.cmd.created")
            .addValue("faction", created.displayName(source))
            .create(source)
    );
  }

  void deleteFaction(CommandSource source, @Argument(F_ARG) Faction faction) {
    Component displayName = faction.displayName(source);

    plugin.getManager().deleteFaction(faction);

    source.sendSuccess(
        Messages.render("factions.cmd.deleted")
            .addValue("faction", displayName)
            .create(source)
    );
  }

  void joinFaction(
      CommandSource source,
      @Argument(F_ARG) Faction faction,
      @Argument("player") User user
  ) throws CommandSyntaxException {
    Faction currentFaction = plugin.getManager().getCurrentFaction(user.getUniqueId());

    if (currentFaction != null) {
      String messageKey;

      if (currentFaction.equals(faction)) {
        messageKey = "same";
      } else {
        messageKey = "other";
      }

      throw Messages.render("factions.errors.alreadyMember", messageKey)
          .addValue("player", user)
          .addValue("faction", currentFaction.displayName(source))
          .exception(source);
    }

    faction.join(user);

    source.sendSuccess(
        Messages.render("factions.cmd.joined")
            .addValue("player", user)
            .addValue("faction", faction.displayName(source))
            .create(source)
    );
  }

  void leaveFaction(
      CommandSource source,
      @Argument("player") User user
  ) throws CommandSyntaxException {
    Faction faction = plugin.getManager().getCurrentFaction(user.getUniqueId());

    if (faction == null) {
      throw FExceptions.notInFaction(source, user);
    }

    faction.leave(user);

    source.sendSuccess(
        Messages.render("factions.cmd.left")
            .addValue("player", user)
            .addValue("faction", faction.displayName(source))
            .create(source)
    );
  }

  void listProperties(
      CommandSource source,
      @Argument(F_ARG) Faction faction
  ) {
    TextComponent.Builder builder = Component.text();
    builder.append(
        Messages.render("factions.cmd.property.list.header")
            .addValue("faction", faction.displayName(source))
            .create(source)
    );

    for (FactionProperty<?> property : Properties.REGISTRY) {
      Object value = faction.get(property);
      boolean unset = value == null || Objects.equals(value, property.getDefaultValue());

      String messageKey = unset ? "unsetValue" : "setValue";

      Component line = Messages.render("factions.cmd.property.list", messageKey)
          .addValue("property", property.getKey())
          .addValue("value", value == null ? "UNSET" : value)
          .create(source);

      builder.append(Component.newline(), line);
    }

    source.sendMessage(builder.build());
  }

  void getProperty(
      CommandSource source,
      @Argument(F_ARG) Faction faction,
      @Argument("property") FactionProperty<Object> property
  ) {
    Object value = faction.get(property);

    source.sendMessage(
        Messages.render("factions.cmd.property.get")
            .addValue("faction", faction.displayName(source))
            .addValue("property", property.getKey())
            .addValue("value", value == null ? "UNSET" : value)
            .create(source)
    );
  }

  void setProperty(
      CommandContext<CommandSource> context,
      @Argument(F_ARG) Faction faction,
      @Argument("property") FactionProperty<Object> property,
      @Argument("input") String input
  ) throws CommandSyntaxException {
    StringRange range = CommandContexts.getNodeRange(context, "input");
    assert range != null;

    StringReader reader = new StringReader(context.getInput());
    reader.setCursor(range.getStart());

    ArgumentType<Object> argumentType = property.getArgumentType();
    Object value = argumentType.parse(reader);

    Commands.ensureCannotRead(reader);

    boolean changed = faction.set(property, value);

    if (!changed) {
      throw Exceptions.format("Nothing changed");
    }

    CommandSource source = context.getSource();

    source.sendSuccess(
        Messages.render("factions.cmd.property.set")
            .addValue("faction", faction.displayName(source))
            .addValue("property", property.getKey())
            .addValue("value", value)
            .create(source)
    );
  }

  CompletableFuture<Suggestions> suggestPropertyValues(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder,
      @Argument("property") FactionProperty<Object> property
  ) {
    return property.getArgumentType().listSuggestions(context, builder);
  }

  void unsetProperty(
      CommandSource source,
      @Argument(F_ARG) Faction faction,
      @Argument(F_ARG) FactionProperty<Object> property
  ) throws CommandSyntaxException {
    boolean changed = faction.set(property, null);

    if (!changed) {
      throw Exceptions.format("Nothing changed");
    }

    source.sendSuccess(
        Messages.render("factions.cmd.property.unset")
            .addValue("faction", faction.displayName(source))
            .addValue("property", property.getKey())
            .create(source)
    );
  }

  private Faction getFaction(CommandSource source, User user, Faction fromArgument)
      throws CommandSyntaxException
  {
    if (fromArgument != null) {
      return fromArgument;
    }

    Faction current = plugin.getManager().getCurrentFaction(user.getUniqueId());
    if (current == null) {
      throw FExceptions.notInFaction(source, user);
    }

    return current;
  }

  private FactionMember getMember(CommandSource source, User user, Faction faction)
      throws CommandSyntaxException
  {
    FactionMember member = faction.getMember(user.getUniqueId());

    if (member == null) {
      throw Messages.render("factions.errors.neverMember")
          .addValue("player", user)
          .addValue("faction", faction.displayName(source))
          .exception(source);
    }

    return member;
  }

  void getReputation(
      CommandSource source,
      @Argument(value = F_ARG, optional = true) Faction fromArgument,
      @Argument("player") User user
  ) throws CommandSyntaxException {
    Faction faction = getFaction(source, user, fromArgument);
    FactionMember member = getMember(source, user, faction);

    int reputation = member.getReputation();
    int base = member.getBaseReputation();

    source.sendMessage(
        Messages.render("factions.cmd.reputation.get")
            .addValue("reputation", reputation)
            .addValue("base", base)
            .addValue("player", user)
            .addValue("faction", faction.displayName(source))
            .create(source)
    );
  }

  Component reputationMessage(
      String key,
      CommandSource source,
      Faction faction,
      User user,
      int old,
      int newV,
      int change
  ) {
    return Messages.render("factions.cmd.reputation", key)
        .addValue("faction", faction.displayName(source))
        .addValue("player", user)
        .addValue("oldValue", old)
        .addValue("newValue", newV)
        .addValue("change", change)
        .create(source);
  }

  void addReputation(
      CommandSource source,
      @Argument(value = F_ARG, optional = true) Faction fromArgument,
      @Argument("player") User user,
      @Argument("amount") int amount
  ) throws CommandSyntaxException {
    Faction faction = getFaction(source, user, fromArgument);
    FactionMember member = getMember(source, user, faction);

    int oldValue = member.getBaseReputation();
    int newValue = oldValue + amount;

    member.setBaseReputation(newValue);

    source.sendSuccess(
        reputationMessage("added", source, faction, user, oldValue, newValue, amount)
    );
  }

  void subtractReputation(
      CommandSource source,
      @Argument(value = F_ARG, optional = true) Faction fromArgument,
      @Argument("player") User user,
      @Argument("amount") int amount
  ) throws CommandSyntaxException {
    Faction faction = getFaction(source, user, fromArgument);
    FactionMember member = getMember(source, user, faction);

    int oldValue = member.getBaseReputation();
    int newValue = oldValue - amount;

    member.setBaseReputation(newValue);

    source.sendSuccess(
        reputationMessage("subtracted", source, faction, user, oldValue, newValue, amount)
    );
  }

  void setReputation(
      CommandSource source,
      @Argument(value = F_ARG, optional = true) Faction fromArgument,
      @Argument("player") User user,
      @Argument("amount") int amount
  ) throws CommandSyntaxException {
    Faction faction = getFaction(source, user, fromArgument);
    FactionMember member = getMember(source, user, faction);

    int oldValue = member.getBaseReputation();

    member.setBaseReputation(amount);

    source.sendSuccess(
        reputationMessage("set", source, faction, user, oldValue, amount, amount)
    );
  }
}
