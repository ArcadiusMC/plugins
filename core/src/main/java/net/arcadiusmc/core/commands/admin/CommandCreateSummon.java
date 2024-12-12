package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.text.Text;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.paper.PaperNbt;
import net.forthecrown.nbt.string.Snbt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

public class CommandCreateSummon extends BaseCommand {

  public CommandCreateSummon() {
    super("createsummon");

    setAliases("create-summon");
    setDescription("Creates a summon command out of an entity");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          Player player = c.getSource().asPlayer();
          Entity target = player.getTargetEntity(5, true);

          if (target == null) {
            throw Exceptions.create("Not looking at any entity");
          }

          return createSummonCommand(c.getSource(), target);
        })

        .then(argument("entity", ArgumentTypes.entity())
            .executes(c -> {
              Entity e = ArgumentTypes.getEntity(c, "entity");
              return createSummonCommand(c.getSource(), e);
            })
        );
  }

  private int createSummonCommand(CommandSource source, Entity entity) {
    NamespacedKey key = entity.getType().getKey();
    CompoundTag tag = PaperNbt.saveEntity(entity);

    tag.remove("UUID");
    tag.remove("WorldUUIDLeast");
    tag.remove("WorldUUIDMost");
    tag.remove("Pos");
    tag.remove("Paper.OriginWorld");
    tag.remove("Paper.SpawnReason");
    tag.remove("Paper.Origin");

    String tagString = Snbt.toString(tag, false, true);
    String command = String.format("/summon %s ~ ~ ~ %s", key, tagString);

    Component txt = Component.text("[Click to copy]", NamedTextColor.GREEN)
        .insertion(tagString)
        .clickEvent(ClickEvent.copyToClipboard(command))
        .hoverEvent(Component.text(command));

    source.sendMessage(
        Text.format("{0} summon command: {1}",
            NamedTextColor.GRAY,
            entity.teamDisplayName(),
            txt
        )
    );

    return SINGLE_SUCCESS;
  }
}
