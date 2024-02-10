package net.arcadiusmc.usables.conditions;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import net.arcadiusmc.Loggers;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.BuiltType;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.utils.io.FtcCodecs;
import net.arcadiusmc.utils.io.Results;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public class TestWorld implements Condition {

  public static final ObjectType<TestWorld> TYPE = BuiltType.<TestWorld>builder()
      .parser((reader, source) -> new TestWorld(ArgumentTypes.world().parse(reader)))
      .suggester((context, builder) -> ArgumentTypes.world().listSuggestions(context,builder))

      .saver((value, ops) -> {
        if (value.world == null) {
          return Results.error("No world set???");
        }
        return FtcCodecs.NAMESPACED_KEY.encodeStart(ops, value.world);
      })

      .loader(dynamic -> {
        if (dynamic == null) {
          return Results.error("No data given");
        }

        return dynamic.decode(FtcCodecs.NAMESPACED_KEY)
            .map(Pair::getFirst)
            .flatMap(key -> {
              World world = Bukkit.getWorld(key);
              if (world == null) {
                return Results.error("No world named '%s'", key);
              }
              return DataResult.success(world);
            })
            .map(TestWorld::new);
      })

      .build();

  private final NamespacedKey world;

  public TestWorld(World world) {
    if (world == null) {
      Loggers.getLogger().warn("Found unknown world while creating world usage test!");
      this.world = null;
    } else {
      this.world = world.getKey();
    }
  }

  @Override
  public boolean test(Interaction interaction) {
    if (world == null) {
      return false;
    }

    return interaction.player().getWorld().getKey().equals(world);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    if (world == null) {
      return Component.text("Cannot use this in this world", NamedTextColor.GRAY);
    }

    return Text.format("Can only use this in the {0}",
        NamedTextColor.GRAY,
        Text.formatWorldName(Bukkit.getWorld(world))
    );
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(world.asString());
  }
}
