package net.arcadiusmc.entity.commands;

import com.badlogic.ashley.core.Entity;
import com.mojang.brigadier.context.CommandContext;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.entity.Entities;
import net.arcadiusmc.entity.EntityPlugin;
import net.arcadiusmc.entity.EntityTemplate;
import net.arcadiusmc.entity.EntityTemplates;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Text;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

public class CommandCustomEntity extends BaseCommand {

  private final EntityPlugin plugin;
  private final RegistryArguments<EntityTemplate> templateArgument;

  public CommandCustomEntity(EntityPlugin plugin) {
    super("customentity");

    setAliases("centity", "custom-entity");
    setDescription("Custom entity command");

    this.templateArgument = new RegistryArguments<>(EntityTemplates.TEMPLATES, "Entity Template");
    this.plugin = plugin;

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("save")
            .executes(c -> {
              plugin.getStorage().saveEntities(plugin.getEngine());

              c.getSource().sendSuccess(
                  Component.text("Saved custom entity plugin", NamedTextColor.GRAY)
              );
              return 0;
            })
        )

        .then(literal("reload")
            .executes(c -> {
              plugin.getEngine().removeAllEntities();
              plugin.getStorage().loadEntities(plugin.getEngine());

              c.getSource().sendSuccess(
                  Component.text("Saved custom entity plugin", NamedTextColor.GRAY)
              );
              return 0;
            })
        )

        .then(literal("summon")
            .then(argument("template", templateArgument)
                .executes(c -> summon(c, false))

                .then(argument("location", ArgumentTypes.position())
                    .executes(c -> summon(c, true))
                )
            )
        );
  }

  private int summon(CommandContext<CommandSource> context, boolean positionSet) {
    CommandSource source = context.getSource();
    Location spawnLocation;

    if (positionSet) {
      spawnLocation = ArgumentTypes.getLocation(context, "location");
    } else {
      spawnLocation = source.getLocation();
    }

    Holder<EntityTemplate> holder = context.getArgument("template", Holder.class);
    EntityTemplate template = holder.getValue();

    Entity entity = template.summon(spawnLocation);
    Entities.getEngine().addEntity(entity);

    source.sendSuccess(
        Text.format("Summoned '&f{0}&r'",
            NamedTextColor.GRAY,
            holder.getKey()
        )
    );
    return 0;
  }
}
