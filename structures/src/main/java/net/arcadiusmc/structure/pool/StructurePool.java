package net.arcadiusmc.structure.pool;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockPalette;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.utils.WeightedList;
import org.slf4j.Logger;

public class StructurePool {

  private static final Logger LOGGER = Loggers.getLogger();

  public static StructurePool EMPTY
      = new StructurePool(Collections.emptyList());

  public static final Codec<StructurePool> CODEC = PoolEntry.CODEC.listOf()
      .xmap(StructurePool::new, StructurePool::getEntries);

  @Getter
  private final ImmutableList<PoolEntry> entries;

  @Getter
  private final int totalWeight;

  public StructurePool(List<PoolEntry> entries) {
    Objects.requireNonNull(entries);

    if (entries.isEmpty()) {
      this.totalWeight = 0;
      this.entries = ImmutableList.of();
    } else {
      this.totalWeight = entries.stream().mapToInt(PoolEntry::weight).sum();
      this.entries = ImmutableList.copyOf(entries);
    }
  }

  public WeightedList<StructureAndPalette> toWeightedList(Registry<BlockStructure> structures) {
    WeightedList<StructureAndPalette> result = new WeightedList<>();

    for (PoolEntry entry : entries) {
      structures.getHolder(entry.structureName())
          .ifPresentOrElse(
              holder -> {
                Map<String, BlockPalette> palettes = holder.getValue().getPalettes();
                String palette = paletteKey(entry.paletteName());

                if (!palettes.containsKey(palette)) {
                  LOGGER.error("Couldn't find palette '{}' in structure '{}'",
                      palette, entry.structureName()
                  );

                  return;
                }

                result.add(entry.weight(), new StructureAndPalette(holder, palette));
              },
              () -> {
                LOGGER.error("Couldn't find structure named '{}' for structure pool",
                    entry.structureName()
                );
              }
          );
    }

    return result;
  }

  public Optional<StructureAndPalette> getRandom(Registry<BlockStructure> structures, Random random) {
    if (isEmpty()) {
      return Optional.empty();
    }

    List<PoolEntry> entries = new ArrayList<>(this.entries);
    Collections.shuffle(entries, random);

    int weightVal = random.nextInt(0, totalWeight);
    int index = 0;

    while (index < entries.size()) {
      PoolEntry entry = entries.get(index);
      weightVal -= entry.weight();

      String palette = paletteKey(entry.paletteName());

      if (weightVal <= 0) {
        return structures.getHolder(entry.structureName())
            .filter(h -> h.getValue().getPalettes().containsKey(palette))
            .map(structure -> new StructureAndPalette(structure, palette));
      }

      index++;
    }

    return Optional.empty();
  }

  public String paletteKey(String key) {
    if (Strings.isNullOrEmpty(key)) {
      return BlockStructure.DEFAULT_PALETTE_NAME;
    }

    return key;
  }

  public boolean isEmpty() {
    return totalWeight < 1 || entries.isEmpty();
  }
}