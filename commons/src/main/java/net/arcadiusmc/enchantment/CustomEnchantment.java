package net.arcadiusmc.enchantment;

import java.util.Set;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class CustomEnchantment extends Enchantment {

  private final NamespacedKey key;

  @Getter
  private final EnchantHandle handle;
  @Getter
  private final String name;

  private final EnchantmentTarget target;
  private final Set<EquipmentSlot> slots;

  public CustomEnchantment(
      @NotNull NamespacedKey key,
      String name,
      EnchantmentTarget type,
      EquipmentSlot... slots
  ) {
    this.key = key;

    this.name = name;
    this.target = type;
    this.slots = Set.of(slots);

    CustomEnchantments.openForRegistrations();
    handle = new EnchantHandle(this, type, slots);
  }

  public CustomEnchantment(@NotNull NamespacedKey key, String name, Enchantment base) {
    this(key, name, base.getItemTarget(), base.getActiveSlots().toArray(new EquipmentSlot[0]));
  }

  public int getWeight() {
    return 10;
  }

  @NotNull
  @Override
  public NamespacedKey getKey() {
    return key;
  }

  @Override
  public int getMaxLevel() {
    return 1;
  }

  @Override
  public int getStartLevel() {
    return 1;
  }

  @Override
  public boolean isTreasure() {
    return false;
  }

  @Override
  public boolean isCursed() {
    return false;
  }

  @Override
  public boolean conflictsWith(@NotNull Enchantment enchantment) {
    return false;
  }

  @Override
  public boolean canEnchantItem(@NotNull ItemStack stack) {
    return true;
  }

  @Override
  public int getMinModifiedCost(int level) {
    return 0;
  }

  @Override
  public int getMaxModifiedCost(int level) {
    return 0;
  }

  @Override
  public @NotNull Component displayName(int i) {
    return Component.text(getName());
  }

  @Override
  public boolean isDiscoverable() {
    return false;
  }

  @Override
  public boolean isTradeable() {
    return false;
  }

  @Override
  public float getDamageIncrease(int level, @NotNull EntityType entityType) {
    return 0;
  }

  @Override
  public @NotNull EnchantmentTarget getItemTarget() {
    return target;
  }

  @Override
  public @NotNull Set<EquipmentSlot> getActiveSlots() {
    return slots;
  }

  @Override
  public @NotNull String translationKey() {
    return null;
  }

  public void onHurt(LivingEntity user, Entity attacker, int level) {
  }

  public void onAttack(LivingEntity user, Entity target, int level) {
  }
}