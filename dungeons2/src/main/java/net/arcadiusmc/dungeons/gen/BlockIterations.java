package net.arcadiusmc.dungeons.gen;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
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
          || iteration.fence == material
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

  @Getter @Setter
  public static class BlockIteration {

    static final Codec<BlockIteration> CODEC
        = ExistingObjectCodec.<BlockIteration>create(builder -> {
          builder.optional("block", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getBlock)
              .setter(BlockIteration::setBlock);

          builder.optional("slab", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getSlab)
              .setter(BlockIteration::setSlab);

          builder.optional("stairs", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getStairs)
              .setter(BlockIteration::setStairs);

          builder.optional("wall", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getWall)
              .setter(BlockIteration::setWall);

          builder.optional("fence", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getFence)
              .setter(BlockIteration::setFence);


          builder.optional("mossy-block", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getMossyBlock)
              .setter(BlockIteration::setMossyBlock);

          builder.optional("mossy-slab", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getMossySlab)
              .setter(BlockIteration::setMossySlab);

          builder.optional("mossy-stairs", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getMossyStairs)
              .setter(BlockIteration::setMossyStairs);

          builder.optional("mossy-wall", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getMossyWall)
              .setter(BlockIteration::setMossyWall);

          builder.optional("mossy-fence", ExtraCodecs.MATERIAL_CODEC)
              .getter(BlockIteration::getMossyWall)
              .setter(BlockIteration::setMossyWall);
        })
        .codec(Codec.unit(BlockIteration::new));

    public static final Codec<List<BlockIteration>> LIST_CODEC = CODEC.listOf();

    private Material block = null;
    private Material stairs = null;
    private Material slab = null;
    private Material wall = null;
    private Material fence = null;

    private Material mossyBlock = null;
    private Material mossyStairs = null;
    private Material mossySlab = null;
    private Material mossyWall = null;
    private Material mossyFence = null;

    public BlockIteration() {

    }
  }
}
