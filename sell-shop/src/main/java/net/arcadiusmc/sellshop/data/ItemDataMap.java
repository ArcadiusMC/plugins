package net.arcadiusmc.sellshop.data;

import java.util.Iterator;
import org.bukkit.Material;

public interface ItemDataMap extends Iterable<ItemSellData> {

  ItemSellData getData(Material material);

  Iterator<String> keyIterator();
}
