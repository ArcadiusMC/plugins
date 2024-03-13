package net.arcadiusmc.usables.actions;

import com.mojang.serialization.DataResult;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.BuiltType;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.Usables;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public class TextAction implements Action {

  public static final ObjectType<TextAction> TYPE = BuiltType.<TextAction>builder()
      .parser((reader, source) -> {
        var remain = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return new TextAction(remain);
      })
      .saver((value, ops) -> DataResult.success(ops.createString(value.getText())))
      .loader(dynamic -> dynamic.asString().map(TextAction::new))
      .build();

  private final String text;

  @Override
  public void onUse(Interaction interaction) {
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      return;
    }

    Player player = playerOpt.get();
    Component message = Usables.formatString(text, player, interaction.getContext());

    player.sendMessage(message);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Usables.formatBaseString(text, null);
  }
}
