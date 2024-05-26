package net.arcadiusmc.utils.inventory;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import net.arcadiusmc.text.Text;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * The very base of an item builder implementing any and all methods that all item types allow
 *
 * @see ItemStacks For the item builder static constructors
 * @see PotionItemBuilder For an implementation of this class focused around potion items
 * @see SkullItemBuilder For no implementation of this class focused around player skull items
 * @see DefaultItemBuilder For the default implementation of this class
 */
@Getter
public abstract class ItemBuilder<T extends ItemBuilder<T>> implements Cloneable {

  protected final ItemStack stack;
  protected ItemMeta baseMeta;

  private boolean ignoreEnchantRestrictions = true;

  ItemBuilder(Material material, int amount) {
    this.stack = new ItemStack(material);
    setAmount(amount);

    baseMeta = Bukkit.getItemFactory()
        .getItemMeta(material);
  }

  ItemBuilder(ItemStack stack, ItemMeta baseMeta) {
    this.stack = stack;
    this.baseMeta = baseMeta;
  }

  protected abstract T getThis();

  public T setAmount(int amount) {
    Validate.isTrue(
        amount >= 0 && amount <= stack.getType().getMaxStackSize(),
        "Invalid stack size: %s", amount
    );

    stack.setAmount(amount);
    return getThis();
  }

  /* -------------------------------- LORE -------------------------------- */

  public T addLore(String lore) {
    return addLore(Text.renderString(lore));
  }

  public T addLore(Component lore) {
    Text.splitNewlines(lore).forEach(component -> addLoreRaw(Text.wrapForItems(component)));
    return getThis();
  }

  public T addLore(Iterable<Component> lore) {
    for (Component component : lore) {
      addLore(component);
    }
    return getThis();
  }

  public T setLore(Iterable<Component> lores) {
    return setLore(StreamSupport.stream(lores.spliterator(), false));
  }

  public T setLore(Stream<Component> stream) {
    List<Component> lore = new ArrayList<>();
    stream.forEach(component -> lore.addAll(Text.splitNewlines(Text.wrapForItems(component))));
    return setLoreRaw(lore);
  }

  public T addLoreRaw(Component lore) {
    return addLoreRaw(ObjectLists.singleton(lore));
  }

  public T addLoreRaw(Iterable<Component> lore) {
    if (lore instanceof List<Component> list) {
      return addLoreRaw(list);
    }

    return addLoreRaw(Lists.newArrayList(lore));
  }

  public T addLoreRaw(List<Component> lore) {
    var existing = baseMeta.lore();

    if (existing == null) {
      baseMeta.lore(lore);
    } else {
      existing.addAll(lore);
      baseMeta.lore(existing);
    }

    return getThis();
  }

  public T setLoreRaw(Iterable<Component> lores) {
    return setLoreRaw(
        StreamSupport.stream(lores.spliterator(), false)
    );
  }

  public T setLoreRaw(Stream<Component> stream) {
    var list = stream.toList();
    baseMeta.lore(list);

    return getThis();
  }

  public T clearLore() {
    return setLoreRaw(Stream.empty());
  }

  /* ------------------------------- NAMES -------------------------------- */

  public T setNameRaw(Component name) {
    baseMeta.displayName(name);
    return getThis();
  }

  public T setName(Component name) {
    if (name == null) {
      return setNameRaw(null);
    }

    return setNameRaw(Text.wrapForItems(name));
  }

  public T setName(String name) {
    if (name == null) {
      return setNameRaw(null);
    }

    return setName(Text.stringToItemText(name));
  }

  /* ------------------------------- FLAGS -------------------------------- */

  public T setFlags(ItemFlag... flags) {
    // Clear existing
    baseMeta.getItemFlags().forEach(flag -> {
      baseMeta.removeItemFlags(flag);
    });

    return addFlags(flags);
  }

  public T addFlags(ItemFlag... flags) {
    baseMeta.addItemFlags(flags);
    return getThis();
  }

  /* ----------------------------- ENCHANTS ------------------------------- */

  public T addEnchant(Enchantment enchantment, int level) {
    return addEnchants(Object2IntMaps.singleton(enchantment, level));
  }

  public T addEnchants(Map<Enchantment, Integer> enchantMap) {
    for (var e : enchantMap.entrySet()) {
      if (baseMeta instanceof EnchantmentStorageMeta storageMeta) {
        storageMeta.addStoredEnchant(e.getKey(), e.getValue(), ignoreEnchantRestrictions);
      } else {
        baseMeta.addEnchant(e.getKey(), e.getValue(), ignoreEnchantRestrictions);
      }
    }

    return getThis();
  }

  public T clearEnchants() {
    if (!baseMeta.hasEnchants()) {
      return getThis();
    }

    for (var e : baseMeta.getEnchants().keySet()) {
      baseMeta.removeEnchant(e);
    }

    return getThis();
  }

  public T ignoreEnchantRestrictions(boolean ignoreEnchantRestrictions) {
    this.ignoreEnchantRestrictions = ignoreEnchantRestrictions;
    return getThis();
  }

  /* ----------------------------- MISC ------------------------------ */

  public <X, Z> T addData(@NotNull NamespacedKey key,
                          @NotNull PersistentDataType<X, Z> type,
                          @NotNull Z value
  ) {
    baseMeta.getPersistentDataContainer()
        .set(key, type, value);

    return getThis();
  }

  public T addModifier(Attribute attribute,
                       String name,
                       double amount,
                       AttributeModifier.Operation operation,
                       EquipmentSlot slot
  ) {
    return addModifier(
        attribute,

        new AttributeModifier(
            UUID.randomUUID(),
            name,
            amount,
            operation,
            slot
        )
    );
  }

  public T addModifier(Attribute attribute, AttributeModifier modifier) {
    baseMeta.addAttributeModifier(attribute, modifier);
    return getThis();
  }

  public T setUnbreakable(boolean unbreakable) {
    baseMeta.setUnbreakable(unbreakable);
    return getThis();
  }

  public T setModelData(Integer customModelData) {
    baseMeta.setCustomModelData(customModelData);
    return getThis();
  }

  public T editMeta(Consumer<ItemMeta> consumer) {
    return editMeta(ItemMeta.class, consumer);
  }

  @SuppressWarnings("unchecked")
  public <M extends ItemMeta> T editMeta(Class<M> metaClass,
                                         Consumer<M> consumer
  ) {
    if (metaClass.isAssignableFrom(baseMeta.getClass())) {
      consumer.accept((M) baseMeta);
    }

    return getThis();
  }

  public ItemStack build() {
    ItemStack result = stack.clone();
    result.setItemMeta(baseMeta.clone());
    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T clone() {
    try {
      return (T) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }
}