package net.arcadiusmc.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.arguments.chat.FormatSuggestions;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.Completions;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ColorArgument implements ArgumentType<TextColor> {

  @Override
  public TextColor parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();
    String str;

    if (reader.peek() == '#') {
      reader.skip();
      str = "#" + reader.readUnquotedString();
    } else {
      str = reader.readUnquotedString();
    }

    TextColor color;

    if (str.startsWith("0x")) {
      color = TextColor.fromHexString("#" + str.substring(2));
    } else if (str.startsWith("#")) {
      color = TextColor.fromHexString(str);
    } else {
      color = NamedTextColor.NAMES.value(str);
    }

    if (color == null) {
      reader.setCursor(start);

      throw Messages.MESSAGE_LIST.render("errors.invalidColor")
          .addValue("color", str)
          .exception();
    }

    return color;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    String token = builder.getRemainingLowerCase();

    if (token.startsWith("#")) {
      builder = builder.createOffset(builder.getStart() + 1);
      return Completions.suggest(builder, FormatSuggestions.HEX_2_NAME.keySet());
    }

    if (token.startsWith("0x")) {
      builder = builder.createOffset(builder.getStart() + 2);
      return Completions.suggest(builder, FormatSuggestions.HEX_2_NAME.keySet());
    }

    return Completions.suggest(builder, FormatSuggestions.HEX_2_NAME.values());
  }
}
