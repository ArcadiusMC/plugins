package net.arcadiusmc.usables.actions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.BuiltType;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.ObjectType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
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

  private Component getBaseText(Audience viewer) {
    try {
      StringReader reader = new StringReader(text);
      return Arguments.CHAT.parse(reader).create(viewer);
    } catch (CommandSyntaxException exc) {
      return Text.valueOf(text, viewer);
    }
  }

  @Override
  public void onUse(Interaction interaction) {
    var player = interaction.player();

    PlaceholderRenderer list = Placeholders.newRenderer().useDefaults();

    Component base = getBaseText(player);
    Component rendered = list.render(base, player, interaction.context());

    player.sendMessage(rendered);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return getBaseText(null);
  }
}
