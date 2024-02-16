package net.arcadiusmc.punish;

import static net.kyori.adventure.text.Component.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.List;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

public record Note(String sourceName, String message, Instant date) implements Comparable<Note> {

  public static final Codec<Note> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.fieldOf("source").forGetter(o -> o.sourceName),
            Codec.STRING.fieldOf("message").forGetter(o -> o.message),
            ExtraCodecs.INSTANT.fieldOf("date").forGetter(o -> o.date)
        )
        .apply(instance, Note::new);
  });

  public static void writeNotes(List<Note> notes, TextWriter writer, User noteHolder) {
    writer.write(
        Messages.render("staffNotes.list.header")
            .addValue("player", noteHolder)
            .create(writer.viewer())
    );

    for (int i = 0; i < notes.size(); i++) {
      Note entry = notes.get(i);
      int viewIndex = i + 1;

      writer.newLine();

      writer.write(
          Messages.render("staffNotes.list.format")
              .addValue("index", viewIndex)
              .addValue("player", noteHolder)
              .addValue("note", entry.display(writer.viewer()))
              .create(writer.viewer())
              .clickEvent(ClickEvent.suggestCommand(
                  "/staff-notes remove " + noteHolder.getName() + " " + viewIndex
              ))
      );
    }
  }

  public Component display(Audience viewer) {
    return text(message).hoverEvent(GMessages.noteMetadata(this, viewer));
  }

  @Override
  public int compareTo(@NotNull Note o) {
    return date.compareTo(o.date);
  }
}
