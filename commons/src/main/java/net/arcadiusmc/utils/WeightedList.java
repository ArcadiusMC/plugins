package net.arcadiusmc.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import lombok.Getter;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.block.spawner.SpawnerEntry;

/**
 * A list with weighted entries to provide for randomized lookups where the
 * chance of an entry appearing is dependent on the weight of the entry.
 *
 * @param <T> List's type
 */
public class WeightedList<T> {

  static final int DEFAULT_WEIGHT = 10;

  private final List<Entry<T>> values = new ObjectArrayList<>();

  /**
   * Total weight of all items in the list
   */
  @Getter
  private int totalWeight = 0;

  public static <T> Codec<WeightedList<T>> codec(Codec<T> baseCodec) {
    Codec<Pair<T, Integer>> weightAndValueCodec = Codec
        .mapPair(
            baseCodec.fieldOf("value"),
            Codec.INT.optionalFieldOf("weight", 1)
        )
        .codec();

//    Codec<Pair<T, Integer>> weightEntryCodec = Codec.either(baseCodec, weightAndValueCodec)
//        .xmap(
//            pair -> pair.map(t -> Pair.of(t, DEFAULT_WEIGHT), p -> p),
//            Either::right
//        );

    return weightAndValueCodec.listOf()
        .xmap(
            pairs -> {
              WeightedList<T> list = new WeightedList<>();
              for (Pair<T, Integer> pair : pairs) {
                list.add(pair.getSecond(), pair.getFirst());
              }
              return list;
            },
            list -> {
              List<Pair<T, Integer>> result = new ObjectArrayList<>();
              for (Entry<T> value : list.values) {
                result.add(Pair.of(value.value, value.weight));
              }
              return result;
            }
        );
  }

  public void addAll(WeightedList<T> other) {
    for (var e : other.values) {
      add(e.weight, e.value);
    }
  }

  public void add(int weight, T value) {
    totalWeight += weight;
    values.add(new Entry<>(value, weight));
  }

  private void remove(Entry<T> entry) {
    int index = values.indexOf(entry);

    if (index == -1) {
      return;
    }

    remove(index);
  }

  private void remove(int index) {
    Objects.checkIndex(index, values.size());
    var val = values.remove(index);
    totalWeight -= val.weight;
  }

  public T get(Random random) {
    Entry<T> value = findRandom(random);

    if (value == null) {
      return null;
    }

    return value.value;
  }

  private Entry<T> findRandom(Random random) {
    if (isEmpty()) {
      return null;
    }

    int value = random.nextInt(0, totalWeight);

    for (Entry<T> p : values) {
      if ((value -= p.weight) <= 0) {
        return p;
      }
    }

    return null;
  }

  public void clear() {
    totalWeight = 0;
    values.clear();
  }

  public boolean isEmpty() {
    return values.isEmpty() || totalWeight < 1;
  }

  public int size() {
    return values.size();
  }

  public WeightedIterator<T> iterator(Random random) {
    return new Iter(random);
  }

  @Override
  public String toString() {
    return values.toString();
  }

  /* ----------------------------- SUB CLASSES ------------------------------ */

  public interface WeightedIterator<T> extends Iterator<T> {

    @Override
    T next();

    @Override
    boolean hasNext();

    int weight();
  }

  private record Entry<T>(T value, int weight) {

  }

  private class Iter implements WeightedIterator<T> {

    private final Random random;
    private final WeightedList<T> remaining;

    private Entry<T> current;

    public Iter(Random random) {
      this.random = random;

      remaining = new WeightedList<>();
      remaining.addAll(WeightedList.this);
    }

    @Override
    public boolean hasNext() {
      return !remaining.isEmpty();
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      if (remaining.values.size() == 1) {
        current = remaining.values.get(0);
        remaining.clear();

        return current.value;
      }

      current = remaining.findRandom(random);
      remaining.remove(current);

      return current.value;
    }

    @Override
    public int weight() {
      if (current == null) {
        return -1;
      }

      return current.weight;
    }

    @Override
    public void remove() {
      if (current == null) {
        throw new IllegalStateException();
      }

      WeightedList.this.remove(current);
      current = null;
    }
  }
}