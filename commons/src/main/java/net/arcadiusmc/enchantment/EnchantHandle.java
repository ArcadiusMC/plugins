package net.arcadiusmc.enchantment;

import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EnchantHandle extends Enchantment {

  private final CustomEnchantment wrapper;

  protected EnchantHandle(
      CustomEnchantment wrapper,
      EnchantmentTarget type,
      EquipmentSlot... slotTypes
  ) {
    super(
        Enchantment.definition(
            toTagKey(type),
            wrapper.getWeight(),
            wrapper.getMaxLevel(),
            new Cost(0, 0),
            new Cost(0, 0),
            0,
            fromBukkitSlots(slotTypes)
        )
    );
    this.wrapper = wrapper;
  }

  static TagKey<Item> toTagKey(EnchantmentTarget target) {
    return switch (target) {
      case ALL -> throw new IllegalStateException("ALL is no longer supported in vanilla");

      case BOW -> ItemTags.BOW_ENCHANTABLE;
      case TOOL -> ItemTags.MINING_ENCHANTABLE;
      case ARMOR, WEARABLE -> ItemTags.ARMOR_ENCHANTABLE;
      case TRIDENT -> ItemTags.TRIDENT_ENCHANTABLE;
      case CROSSBOW -> ItemTags.CROSSBOW_ENCHANTABLE;
      case BREAKABLE -> ItemTags.DURABILITY_ENCHANTABLE;
      case ARMOR_FEET -> ItemTags.FOOT_ARMOR_ENCHANTABLE;
      case ARMOR_HEAD -> ItemTags.HEAD_ARMOR_ENCHANTABLE;
      case ARMOR_LEGS -> ItemTags.LEG_ARMOR_ENCHANTABLE;
      case VANISHABLE -> ItemTags.VANISHING_ENCHANTABLE;
      case ARMOR_TORSO -> ItemTags.CHEST_ARMOR_ENCHANTABLE;
      case FISHING_ROD -> ItemTags.FISHING_ENCHANTABLE;

      default -> ItemTags.WEAPON_ENCHANTABLE;
    };
  }

  static net.minecraft.world.entity.EquipmentSlot[] fromBukkitSlots(EquipmentSlot[] slots) {
    net.minecraft.world.entity.EquipmentSlot[] result
        = new net.minecraft.world.entity.EquipmentSlot[slots.length];

    for (int i = 0; i < slots.length; i++) {
      result[i] = CraftEquipmentSlot.getNMS(slots[i]);
    }

    return result;
  }

  @Override
  public Component getFullname(int level) {
    return PaperAdventure.asVanilla(wrapper.displayName(level));
  }

  @Override
  public boolean isTreasureOnly() {
    return wrapper.isTreasure();
  }

  @Override
  public boolean isTradeable() {
    return wrapper.isTradeable();
  }

  @Override
  public boolean isDiscoverable() {
    return wrapper.isDiscoverable();
  }

  @Override
  public boolean isCurse() {
    return wrapper.isCursed();
  }

  @Override
  public boolean canEnchant(@NotNull ItemStack itemstack) {
    return wrapper.canEnchantItem(CraftItemStack.asBukkitCopy(itemstack));
  }

  @Override
  public void doPostAttack(LivingEntity user, Entity target, int level) {
    wrapper.onAttack(
        user.getBukkitLivingEntity(),
        target.getBukkitEntity(),
        level
    );
  }

  @Override
  public void doPostHurt(LivingEntity user, @Nullable Entity attacker, int level) {
    wrapper.onHurt(
        user.getBukkitLivingEntity(),
        attacker == null ? null : attacker.getBukkitEntity(),
        level
    );
  }
}