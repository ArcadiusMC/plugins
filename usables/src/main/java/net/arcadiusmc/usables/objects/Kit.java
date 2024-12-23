package net.arcadiusmc.usables.objects;

import java.util.Collection;
import java.util.Objects;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.utils.inventory.ItemArrayList;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.TagUtil;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Kit extends CommandUsable {

  private final ItemList items = new ItemArrayList();

  public Kit(String name) {
    super(name);
  }

  @Override
  protected void onInteract(Player player, boolean adminInteraction) {
    ItemStacks.giveOrDrop(player.getInventory(), items);
  }

  @Override
  public String getCommandPrefix() {
    return "/kit " + getName();
  }

  @Override
  public void save(CompoundTag tag) {
    super.save(tag);

    tag.put("items", TagUtil.writeList(items, TagUtil::writeItem));
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);

    var itemList = TagUtil.readList(tag.get("items"), TagUtil::readItem);
    setItems(itemList);
  }

  @Override
  public void write(TextWriter writer) {
    writer.field("Items");
    writer.newLine();
    for (ItemStack item : items) {
      writer.formattedLine("- {0, item, -amount}", item);
    }
  }

  public ItemList getItems() {
    return ItemLists.cloneAllItems(items);
  }

  public void setItems(Collection<ItemStack> items) {
    Objects.requireNonNull(items, "Null items");

    this.items.clear();
    this.items.addAll(ItemLists.cloneAllItems(items));
  }
}
