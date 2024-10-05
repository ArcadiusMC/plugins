package net.arcadiusmc.dungeons.gen;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.Material;
import org.slf4j.Logger;

public class BlockIterations {

  private static final Logger LOGGER = Loggers.getLogger();
  private static final List<BlockIteration> iterations = new ObjectArrayList<>();

  public static BlockIteration getIteration(Material material) {
    if (iterations.isEmpty()) {
      return null;
    }

    for (BlockIteration iteration : iterations) {
      if (iteration.block == material
          || iteration.slab == material
          || iteration.stairs == material
          || iteration.wall == material
      ) {
        return iteration;
      }
    }

    return null;
  }

  static {
    loadIterations();
  }

  private static void loadIterations() {
    String resourceName = "block-iterations.json";
    URL iterationsUrl = BlockIterations.class.getClassLoader().getResource(resourceName);

    if (iterationsUrl == null) {
      LOGGER.error("Failed to load {}: Doesn't exist", resourceName);
      return;
    }

    JsonElement el;

    try (InputStream stream = iterationsUrl.openStream()) {
      InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
      el = JsonParser.parseReader(reader);
    } catch (IOException | JsonSyntaxException | JsonIOException e) {
      LOGGER.error("Failed to load {}:", resourceName, e);
      return;
    }

    BlockIteration.LIST_CODEC.parse(JsonOps.INSTANCE, el)
        .mapError(s -> "Failed to read " + resourceName + ": " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(blockIterations -> {
          iterations.clear();
          iterations.addAll(blockIterations);
        });
  }


  public record BlockIteration(Material block, Material stairs, Material slab, Material wall) {

    static final Codec<BlockIteration> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.MATERIAL_CODEC.fieldOf("block")
                  .forGetter(BlockIteration::block),

              ExtraCodecs.MATERIAL_CODEC.optionalFieldOf("stairs")
                  .forGetter(b -> Optional.ofNullable(b.stairs)),

              ExtraCodecs.MATERIAL_CODEC.optionalFieldOf("slab")
                  .forGetter(b -> Optional.ofNullable(b.slab)),

              ExtraCodecs.MATERIAL_CODEC.optionalFieldOf("wall")
                  .forGetter(b -> Optional.ofNullable(b.wall))
          )
          .apply(instance, (block, stairs, slab, wall) -> {
            return new BlockIteration(
                block,
                stairs.orElse(null),
                slab.orElse(null),
                wall.orElse(null)
            );
          });
    });

    static final Codec<List<BlockIteration>> LIST_CODEC = CODEC.listOf();
  }
}
