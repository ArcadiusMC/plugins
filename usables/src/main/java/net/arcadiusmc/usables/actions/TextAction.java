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
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.command.arguments.chat.MessageSuggestions;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.Usables;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public class TextAction implements Action {

  public static final ObjectType<TextAction> TEXT_TYPE = new TextActionType(false);
  public static final ObjectType<TextAction> ACTIONBAR_TYPE = new TextActionType(true);

  private final TextActionType type;
  private final String text;

  @Override
  public void onUse(Interaction interaction) {
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      return;
    }

    Player player = playerOpt.get();
    Component message = Usables.formatString(text, player, interaction.getContext());

    if (type.actionbar) {
      player.sendActionBar(message);
    } else {
      player.sendMessage(message);
    }
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return type;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Usables.formatBaseString(text, null);
  }
}

class TextActionType implements ObjectType<TextAction> {

  final boolean actionbar;

  public TextActionType(boolean actionbar) {
    this.actionbar = actionbar;
  }

  @Override
  public TextAction parse(StringReader reader, CommandSource source) throws CommandSyntaxException {
    String remain = reader.getRemaining();
    reader.setCursor(reader.getTotalLength());
    return new TextAction(this, remain);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    return MessageSuggestions.get(context, builder);
  }

  @Override
  public <S> DataResult<TextAction> load(Dynamic<S> dynamic) {
    return dynamic.asString().map(s -> new TextAction(this, s));
  }

  @Override
  public <S> DataResult<S> save(@NotNull TextAction value, @NotNull DynamicOps<S> ops) {
    return DataResult.success(ops.createString(value.getText()));
  }
}