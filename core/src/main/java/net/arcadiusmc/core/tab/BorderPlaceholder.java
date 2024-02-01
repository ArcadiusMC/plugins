package net.arcadiusmc.core.tab;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.core.tab.TabConfig.DynamicBorderConfig;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.ParsedPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ArrayArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class BorderPlaceholder implements ParsedPlaceholder {

  static final ArrayArgument<TextColor> COLOR_ARGUMENT = ArgumentTypes.array(Arguments.COLOR);

  final int charWidth;
  final DynamicBorderConfig config;

  @Override
  public @Nullable Component render(StringReader reader, PlaceholderContext context)
      throws CommandSyntaxException {
    if (!reader.canRead()) {
      return config.createText(charWidth);
    }

    List<TextColor> colors = COLOR_ARGUMENT.parse(reader);
    String borderString = config.create(charWidth);

    return Text.gradient(borderString, true, colors.toArray(TextColor[]::new))
        .style(config.style());
  }
}
