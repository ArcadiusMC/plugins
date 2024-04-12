package net.arcadiusmc.holograms.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.arguments.chat.MessageSuggestions;
import net.arcadiusmc.holograms.Hologram;
import net.arcadiusmc.holograms.HologramPlugin;
import net.arcadiusmc.holograms.ServiceImpl;
import net.arcadiusmc.holograms.TextImpl;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.forthecrown.grenadier.annotations.VariableInitializer;

@CommandFile("holograms.gcn")
public class CommandHologram extends DisplayCommand {

  public CommandHologram(HologramPlugin plugin) {
    super(plugin.getService(), "holograms", "/holograms list %s %s");
  }

  @VariableInitializer
  void initVars(Map<String, Object> map) {
    super.initVariables(map);
    map.put("display", new HologramArgument(service));
    map.put("holo", new HologramArgument(service));
  }

  @Override
  Collection<? extends Hologram> getList(ServiceImpl service) {
    return service.getHolograms();
  }

  void createHologram(
      CommandSource source,
      @Argument("name") String name
  ) throws CommandSyntaxException {
    if (service.getHologram(name).isPresent()) {
      throw Messages.render("holograms.errors.alreadyExists")
          .addValue("name", name)
          .exception(source);
    }

    TextImpl text = new TextImpl(name);
    service.addHologram(text);

    source.sendSuccess(
        Messages.render("holograms.created")
            .addValue("board", text.displayName())
            .create(source)
    );
  }

  void removeHologram(
      CommandSource source,
      @Argument("hologram") TextImpl hologram
  ) {
    if (hologram.isSpawned()) {
      hologram.kill();
    }

    service.removeHologram(hologram.getName());

    source.sendSuccess(
        Messages.render("holograms.removed")
            .addValue("board", hologram.displayName())
            .create(source)
    );
  }

  void copyHologram(
      CommandSource source,
      @Argument("from") TextImpl copySource,
      @Argument("hologram") TextImpl target
  ) throws CommandSyntaxException {
    if (Objects.equals(target, copySource)) {
      throw Messages.render("holograms.errors.copySelf")
          .addValue("board", target.displayName())
          .exception(source);
    }

    target.copyFrom(copySource);

    if (target.isSpawned()) {
      target.update();
    }

    source.sendSuccess(
        Messages.render("holograms.copied")
            .addValue("source", copySource.displayName())
            .addValue("target", target.displayName())
            .create(source)
    );
  }

  void setContent(
      CommandSource source,
      @Argument("hologram") TextImpl text,
      @Argument("text") String string
  ) {
    text.setText(string);

    if (text.isSpawned()) {
      text.update();
    }

    source.sendSuccess(
        Messages.render("holograms.text.set")
            .addValue("board", text.displayName())
            .addValue("value", text.renderText(source))
            .create(source)
    );
  }

  CompletableFuture<Suggestions> suggestChat(
      CommandContext<CommandSource> c,
      SuggestionsBuilder builder
  ) {
    return MessageSuggestions.get(c, builder, true);
  }
}
