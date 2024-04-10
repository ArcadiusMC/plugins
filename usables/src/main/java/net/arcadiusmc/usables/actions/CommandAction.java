package net.arcadiusmc.usables.actions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CommandAction implements Action {

  public static final ObjectType<CommandAction> AS_PLAYER = new CommandActionType(true);
  public static final ObjectType<CommandAction> AS_SELF = new CommandActionType(false);

  private final String command;
  private final boolean asPlayer;

  public CommandAction(String command, boolean asPlayer) {
    this.command = command;
    this.asPlayer = asPlayer;
  }

  static String formatPlaceholders(String cmd, Player player) {
    return Commands.replacePlaceholders(cmd, player);
  }

  @Override
  public void onUse(Interaction interaction) {
    String formattedCommand;
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      if (asPlayer) {
        return;
      }

      formattedCommand = command;
    } else {
      var player = playerOpt.get();
      formattedCommand = formatPlaceholders(command, player);

      if (asPlayer) {
        player.performCommand(formattedCommand);
        return;
      }
    }

    CommandSender sender = interaction.getObject().getCommandSender();
    CommandSource source = optionallyMute(sender, interaction);

    Grenadier.dispatch(source, formattedCommand);
  }

  private CommandSource optionallyMute(CommandSender base, Interaction interaction) {
    CommandSource source = Grenadier.createSource(base);

    if (!interaction.getBoolean("silent").orElse(false)) {
      return source;
    }

    return source.silent();
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return asPlayer ? AS_PLAYER : AS_SELF;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(command);
  }
}

class CommandActionType implements ObjectType<CommandAction> {

  private final boolean asPlayer;

  public CommandActionType(boolean asPlayer) {
    this.asPlayer = asPlayer;
  }

  @Override
  public CommandAction parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    String remaining = reader.getRemaining();
    reader.setCursor(reader.getTotalLength());
    return new CommandAction(remaining, asPlayer);
  }

  @Override
  public @NotNull <S> DataResult<CommandAction> load(@Nullable Dynamic<S> dynamic) {
    return dynamic.asString().map(s -> new CommandAction(s, asPlayer));
  }

  @Override
  public <S> DataResult<S> save(@NotNull CommandAction value, @NotNull DynamicOps<S> ops) {
    return DataResult.success(ops.createString(value.getCommand()));
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    try {
      return Grenadier.suggestAllCommands().getSuggestions(context, builder);
    } catch (CommandSyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
