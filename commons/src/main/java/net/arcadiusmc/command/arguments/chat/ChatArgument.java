package net.arcadiusmc.command.arguments.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.arguments.chat.MessageArgument.Result;
import net.arcadiusmc.text.ChatEmotes;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.command.arguments.Arguments;
import net.forthecrown.grenadier.internal.SimpleVanillaMapped;
import net.forthecrown.grenadier.internal.VanillaMappedArgument;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.minecraft.commands.CommandBuildContext;

public class ChatArgument
    implements SimpleVanillaMapped, ArgumentType<ViewerAwareMessage>
{

  @Override
  public ViewerAwareMessage parse(StringReader reader) throws CommandSyntaxException {
    char peek = reader.canRead() ? reader.peek() : 'a';

    if (peek == '{' || peek == '[' || peek == '"') {
      var result = ArgumentTypes.component().parse(reader);
      return ViewerAwareMessage.wrap(ChatEmotes.format(result));
    }

    if (peek == '\\') {
      reader.skip();
    }

    Result result = Arguments.MESSAGE.parse(reader);
    return result.toPlayerMessage();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    return MessageSuggestions.get(context, builder, true);
  }

  @Override
  public ArgumentType<?> getVanillaType() {
    return Arguments.MESSAGE.getVanillaType();
  }
}