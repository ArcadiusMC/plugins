package net.arcadiusmc.items;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry.Builder;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry.EnchantmentCost;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.event.WritableRegistry;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.tag.TagKey;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("experimental")
public class ItemsBootstrapper implements PluginBootstrap {

  @Override
  public void bootstrap(@NotNull BootstrapContext context) {
    LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();

    manager.registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> {
      initEnchants(event.registry(), event::getOrCreateTag);
    }));
  }

  private void initEnchants(
      WritableRegistry<Enchantment, Builder> registry,
      Function<TagKey<ItemType>, RegistryKeySet<ItemType>> tags
  ) {
    // Soulbound
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_SOULBOUND),

        builder -> {
          builder.description(Component.text("Soulbound"))
              .activeSlots(EquipmentSlotGroup.ANY)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(1)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.ENCHANTABLE_EQUIPPABLE));
        }
    );

    // Pirate's Luck
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_PIRATES_LUCK),

        builder -> {
          builder.description(Component.text("Pirate's Luck"))
              .activeSlots(EquipmentSlotGroup.MAINHAND)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(5)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.SHOVELS));
        }
    );

    // Imperial Duping
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_DUPING),

        builder -> {
          builder.description(Component.text("Imperial Duping"))
              .activeSlots(EquipmentSlotGroup.MAINHAND)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(4)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.PICKAXES));
        }
    );

    // Battle call
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_BATTLE_CALL),

        builder -> {
          builder.description(Component.text("Battle Call"))
              .activeSlots(EquipmentSlotGroup.MAINHAND)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(3)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.AXES));
        }
    );

    // Slice
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_SLICE),

        builder -> {
          builder.description(Component.text("Slice"))
              .activeSlots(EquipmentSlotGroup.MAINHAND)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(3)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.ENCHANTABLE_SWORD));
        }
    );

    // Cutting mastery
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_CUTTING_MASTERY),

        builder -> {
          builder.description(Component.text("Cutting Mastery"))
              .activeSlots(EquipmentSlotGroup.MAINHAND)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(5)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.AXES));
        }
    );

    // Strong Aim
    registry.register(
        TypedKey.create(RegistryKey.ENCHANTMENT, EnchantKeys.KEY_STRONG_AIM),

        builder -> {
          builder.description(Component.text("Strong Aim"))
              .activeSlots(EquipmentSlotGroup.MAINHAND)
              .maximumCost(EnchantmentCost.of(1, 1))
              .minimumCost(EnchantmentCost.of(1, 1))
              .anvilCost(1)
              .maxLevel(1)
              .weight(10)
              .supportedItems(tags.apply(ItemTypeTagKeys.ENCHANTABLE_BOW));
        }
    );
  }
}
