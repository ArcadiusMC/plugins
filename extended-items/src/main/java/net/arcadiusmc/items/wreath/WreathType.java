package net.arcadiusmc.items.wreath;

import java.util.concurrent.TimeUnit;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.lore.EnchantsLoreElement;
import net.arcadiusmc.items.lore.FlavourTextElement;
import net.arcadiusmc.items.lore.LevelLore;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.upgrade.AddAttributeMod;
import net.arcadiusmc.items.upgrade.AddEnchantMod;
import net.arcadiusmc.items.upgrade.ItemUpgrades;
import net.arcadiusmc.items.upgrade.ModelDataMod;
import net.arcadiusmc.items.upgrade.UpgradeFunction;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class WreathType implements ItemType {

  static final int MAX_LEVEL = 5;

  static final int MODEL_DATA_LEVEL_1 = 10020001;
  static final int MODEL_DATA_LEVEL_2 = 10020001;
  static final int MODEL_DATA_LEVEL_3 = 10020002;
  static final int MODEL_DATA_LEVEL_4 = 10020002;
  static final int MODEL_DATA_LEVEL_5 = 10020003;

  static final int REGEN_DURATION_TICKS = 220;
  static final int RESIST_DURATION_TICKS = 220;

  static final int RESIST_LEVEL = 4;
  static final int REGEN_LEVEL = 3;

  static final String REGEN_MSG  = "itemsPlugin.wreath.damageRegen";
  static final String RESIST_MSG = "itemsPlugin.wreath.damageResist";

  static final String ATTR_ID_ARMOR_TOUGHNESS = "wreath.armor_toughness";
  static final String ATTR_ID_ARMOR           = "wreath.armor";
  static final String ATTR_ID_KB_RES          = "wreath.knockback_resistance";
  static final String ATTR_ID_HEALTH          = "wreath.health_boost";

  static final ItemUpgrades UPGRADES = createUpgrades();

  static final LevelLore[] FLAVOUR_TEXTS = {
      new LevelLore(
          1,
          (item, writer) -> {
            writer.line(Messages.renderText("itemsPlugin.wreath.flavour"));
          }
      )
  };

  static final LoreElement NEXT_RANK_TEXT = LoreElement.ifNotMaxLevel((item, writer) -> {
    writer.line(Messages.renderText("itemsPlugin.wreath.nextRank"));
  });

  static final LoreElement EMPTY_LINE_IF_NOT_MAX_LEVEL
      = LoreElement.ifNotMaxLevel(LoreElement.EMPTY_LINE);

  @Override
  public ItemStack createBaseItem() {
    DefaultItemBuilder builder = ItemStacks.builder(Material.BAMBOO_BUTTON)
        .setName(Messages.renderText("itemsPlugin.wreath.name"))
        .addFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
        .setUnbreakable(true);

    ItemStack itemStack = builder.build();
    itemStack.editMeta(meta -> {
      CompoundTag unhandledData = ItemStacks.getUnhandledTags(meta);
      unhandledData.putByte("wearable", 1);
      ItemStacks.setUnhandledTags(meta, unhandledData);
    });

    return itemStack;
  }

  @Override
  public void addComponents(ExtendedItem item) {
    Level level = new Level(MAX_LEVEL);
    OwnerLore ownerLore = new OwnerLore();

    PostDamageEffect regenAfterDmg = new PostDamageEffect(
        PotionEffectType.REGENERATION,
        REGEN_DURATION_TICKS,
        REGEN_LEVEL,
        TimeUnit.SECONDS.toMillis(30)
    );

    PostDamageEffect resistAfterDmg = new PostDamageEffect(
        PotionEffectType.RESISTANCE,
        RESIST_DURATION_TICKS,
        RESIST_LEVEL,
        TimeUnit.SECONDS.toMillis(30)
    );

    item.addComponent(level);
    item.addComponent(new Owner());
    item.addComponent(UPGRADES.createComponent());
    item.addComponent(regenAfterDmg);
    item.addComponent(resistAfterDmg);
    item.addComponent(ownerLore);

    item.addLore(EnchantsLoreElement.ENCHANTS);
    item.addLore(LoreElement.EMPTY_LINE);
    item.addLore(level);
    item.addLore(createFlavourText());
    item.addLore(NEXT_RANK_TEXT);
    item.addLore(EMPTY_LINE_IF_NOT_MAX_LEVEL);
    item.addLore(UPGRADES.createPreviewElement());
    item.addLore(LoreElement.ifNotMaxLevel(LoreElement.BORDER));
    item.addLore(ownerLore);
    item.addLore(LoreElement.EMPTY_LINE);
    item.addLore(UPGRADES.createStatusElement());
  }

  private FlavourTextElement createFlavourText() {
    return new FlavourTextElement(FLAVOUR_TEXTS, LoreElement.BORDER);
  }

  private static Component postDamageEffectText(String key, int tickDuration) {
    return Messages.render(key)
        .addValue("duration", Ticks.duration(tickDuration))
        .asComponent();
  }

  private static ItemUpgrades createUpgrades() {
    return ItemUpgrades.builder()

        .statusPrefix((item, writer) -> {
          writer.line(Messages.renderText("itemsPlugin.wreath.onHead"));
        })

        .level(1, level -> {
          level
              .upgrade(new ModelDataMod(MODEL_DATA_LEVEL_1))

              .upgrade(new AddEnchantMod(Enchantment.PROTECTION, 4))
              .upgrade(new AddEnchantMod(Enchantment.AQUA_AFFINITY, 1))
              .upgrade(new AddEnchantMod(Enchantment.RESPIRATION, 3))

              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_ARMOR_TOUGHNESS,
                  ATTR_ID_ARMOR_TOUGHNESS,
                  Operation.ADD_NUMBER,
                  3
              ), l -> l.statusPersists(false))
              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_ARMOR,
                  ATTR_ID_ARMOR,
                  Operation.ADD_NUMBER,
                  3
              ))
              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                  ATTR_ID_KB_RES,
                  Operation.ADD_NUMBER,
                  0.1
              ), l -> l.statusPersists(false));
        })

        .level(2, level -> {
          level
              .upgrade(new ModelDataMod(MODEL_DATA_LEVEL_2))

              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_ARMOR_TOUGHNESS,
                  ATTR_ID_ARMOR_TOUGHNESS,
                  Operation.ADD_NUMBER,
                  3.5
              ), l -> l.statusPersists(false))
              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                  ATTR_ID_KB_RES,
                  Operation.ADD_NUMBER,
                  0.15
              ), l -> l.statusPersists(false))
              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_MAX_HEALTH,
                  ATTR_ID_HEALTH,
                  Operation.ADD_NUMBER,
                  10
              ));
        })

        .level(3, level -> {
          level
              .upgrade(new ModelDataMod(MODEL_DATA_LEVEL_3))

              .upgrade(new AddEnchantMod(Enchantment.PROJECTILE_PROTECTION, 4))

              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_ARMOR_TOUGHNESS,
                  ATTR_ID_ARMOR_TOUGHNESS,
                  Operation.ADD_NUMBER,
                  4
              ), l -> l.statusPersists(false))
              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                  ATTR_ID_KB_RES,
                  Operation.ADD_NUMBER,
                  0.2
              ), l -> l.statusPersists(false))

              .upgrade(UpgradeFunction.EMPTY, upgrade -> {
                upgrade.statusPersists(true)
                    .previewText((item, writer) -> {
                      writer.line(postDamageEffectText(REGEN_MSG, REGEN_DURATION_TICKS));
                    })
                    .statusText((item, writer) -> {
                      Component text = postDamageEffectText(REGEN_MSG, REGEN_DURATION_TICKS);
                      writer.line("+", NamedTextColor.BLUE);
                      writer.write(text.color(NamedTextColor.BLUE));
                    });
              });
        })

        .level(4, level -> {
          level
              .upgrade(new ModelDataMod(MODEL_DATA_LEVEL_4))
              .upgrade(new AddEnchantMod(Enchantment.BLAST_PROTECTION, 4))

              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_ARMOR_TOUGHNESS,
                  ATTR_ID_ARMOR_TOUGHNESS,
                  Operation.ADD_NUMBER,
                  4.5
              ))
              .upgrade(new AddAttributeMod(
                  Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                  ATTR_ID_KB_RES,
                  Operation.ADD_NUMBER,
                  0.25
              ))

              .upgrade(UpgradeFunction.EMPTY, upgrade -> {
                upgrade.statusPersists(true)
                    .previewText((item, writer) -> {
                      writer.line(postDamageEffectText(RESIST_MSG, RESIST_DURATION_TICKS));
                    })
                    .statusText((item, writer) -> {
                      Component text = postDamageEffectText(RESIST_MSG, RESIST_DURATION_TICKS);
                      writer.line("+", NamedTextColor.BLUE);
                      writer.write(text.color(NamedTextColor.BLUE));
                    });
              });
        })

        .level(5, level -> {
          level
              .upgrade(new ModelDataMod(MODEL_DATA_LEVEL_5))
              .upgrade(new AddEnchantMod(Enchantment.FIRE_PROTECTION, 4));
        })

        .build();
  }

  @Override
  public boolean isPersistentBeyondDeath() {
    return true;
  }
}
