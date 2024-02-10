package net.arcadiusmc.sellshop.data;

import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class CombinedDataMap implements ItemDataMap {

  private final Set<ItemDataMap> maps = new HashSet<>();

  public boolean addSource(ItemDataMap map) {
    return maps.add(map);
  }

  public boolean removeSource(ItemDataMap map) {
    return maps.remove(map);
  }

  public void clearSources() {
    maps.clear();
  }

  @Override
  public ItemSellData getData(Material material) {
    for (ItemDataMap map : maps) {
      ItemSellData data = map .getData(material);

      if (data == null) {
        continue;
      }

      return data;
    }

    return null;
  }

  @NotNull
  @Override
  public Iterator<ItemSellData> iterator() {
    return new Itr(maps.iterator());
  }

  @Override
  public Iterator<String> keyIterator() {
    List<Iterator<String>> keys = new ArrayList<>();
    for (ItemDataMap map : maps) {
      keys.add(map.keyIterator());
    }
    return Iterators.concat(keys.iterator());
  }

  public boolean hasSource(ItemDataMap map) {
    return maps.contains(map);
  }

  static class Itr implements Iterator<ItemSellData> {

    private final Iterator<ItemDataMap> mapIt;
    private Iterator<ItemSellData> dataIt;

    public Itr(Iterator<ItemDataMap> mapIt) {
      this.mapIt = mapIt;
    }

    @Override
    public boolean hasNext() {
      if (dataIt != null && dataIt.hasNext()) {
        return true;
      }

      while (mapIt.hasNext()) {
        ItemDataMap nextMap = mapIt.next();
        dataIt = nextMap.iterator();

        if (dataIt.hasNext()) {
          return true;
        }
      }

      return false;
    }

    @Override
    public ItemSellData next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return dataIt.next();
    }
  }
}
