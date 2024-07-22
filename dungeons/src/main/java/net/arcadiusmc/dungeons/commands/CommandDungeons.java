package net.arcadiusmc.dungeons.commands;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.dungeons.DungeonStructure;
import net.arcadiusmc.dungeons.DungeonWorld;
import net.arcadiusmc.dungeons.generator.TreeGenerator;
import net.arcadiusmc.dungeons.generator.TreeGeneratorConfig;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PluginJar;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.slf4j.Logger;

public class CommandDungeons extends BaseCommand {

  private static final Logger LOGGER = Loggers.getLogger();

  public CommandDungeons() {
    super("dungeon-gen");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("generate")
            .executes(c -> {
              Path path = PluginJar.saveResources("test-gen-config.json");
              TreeGeneratorConfig cfg;

              try {
                JsonObject obj = JsonUtils.readFileObject(path);
                cfg = TreeGeneratorConfig.deserialize(obj);
              } catch (IOException exc) {
                LOGGER.error("Failed to load generator config from {}", path, exc);
                throw Exceptions.create("Error loading generator config");
              }

              c.getSource().sendMessage(Component.text("Starting level generation..."));

              TreeGenerator.generateAsync(cfg).whenComplete(new PostGeneration(c.getSource()));
              return 0;
            })
        )

        .then(literal("reset-dungeon-world")
            .executes(c -> {
              DungeonWorld.reset();
              c.getSource().sendSuccess(Component.text("Reset dungeon world"));
              return 0;
            })
        );
  }

  class PostGeneration implements BiConsumer<DungeonStructure, Throwable> {

    final CommandSource source;

    public PostGeneration(CommandSource source) {
      this.source = source;
    }

    @Override
    public void accept(DungeonStructure struct, Throwable throwable) {
      if (throwable != null) {
        source.sendFailure(Component.text("Failed to generate dungeon", NamedTextColor.RED));
        return;
      }

      Tasks.runSync(() -> {
        postGenSync(struct);
      });
    }

    private void postGenSync(DungeonStructure struct) {
      source.sendSuccess(Component.text("Generated structure, starting spawning"));
      World world = DungeonWorld.get();

      if (world == null) {
        try {
          world = DungeonWorld.reset();
        } catch (Throwable t) {
          LOGGER.error("Failed to create world", t);
          return;
        }

        LOGGER.info("reset dungeon world");
      }

      LOGGER.info("post world stuff");

      try {
        LOGGER.info("pre struct.place");
        struct.place(world).whenComplete(this::postPlace);
        LOGGER.info("post struct.place");
      } catch (Throwable t) {
        postPlace(null, t);
      }
    }

    private void postPlace(Void unused, Throwable throwable) {
      if (throwable != null) {
        LOGGER.error("Failed to place structure :(", throwable);
        source.sendFailure(Component.text("Failed to spawn dungeon structure", NamedTextColor.RED));

        return;
      }

      Tasks.runSync(() -> {
        source.sendSuccess(Component.text("Dungeon generated and spawned"));
      });
    }
  }
}
