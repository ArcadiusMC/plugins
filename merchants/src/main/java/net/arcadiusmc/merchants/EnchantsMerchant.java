package net.arcadiusmc.merchants;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.ListTag;
import net.forthecrown.nbt.TagTypes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.slf4j.Logger;

public class EnchantsMerchant extends Merchant {

  private static final Logger LOGGER = Loggers.getLogger();

  static final int NONE = -1;

  static final String TAG_CHOSEN = "already_chosen";
  static final String TAG_CURRENT = "current_enchant";
  static final String TAG_CURRENT_LEVEL = "current_level";
  static final String TAG_CURRENT_price = "current_price";

  static final Slot BOOK_SLOT = Slot.of(4, 1);

  static final Codec<Enchantment> CURRENT_CODEC
      = ExtraCodecs.registryCodec(Registry.ENCHANTMENT);

  Config config = Config.EMPTY;

  final Set<NamespacedKey> alreadyChosen = new HashSet<>();

  private Enchantment current = null;
  private int currentLevel = NONE;
  private int currentPrice = NONE;

  private final Random random;

  public EnchantsMerchant(MerchantsPlugin plugin) {
    super(plugin, "enchants");
    this.random = new Random();
  }

  @Override
  protected void clearData() {
    alreadyChosen.clear();
    current = null;
  }

  @Override
  public void reloadConfig() {
    loadConfig(Config.CODEC, cfg -> this.config = cfg);
    createMenu();
  }

  private void createMenu() {
    MenuBuilder builder = Menus.builder(Menus.sizeFromRows(3))
        .setTitle(config.menuTitle);

    for (int i = 0; i < builder.getSize(); i++) {
      builder.add(i, Menus.defaultBorderItem());
    }

    builder.add(BOOK_SLOT, enchantmentNode());

    menu = builder.build();
  }

  private MenuNode enchantmentNode() {
    return MenuNode.builder()
        .setItem((user, context) -> {
          if (current == null) {
            return ItemStacks.builder(Material.BARRIER)
                .setName("&eOops!")
                .addLore("&7Looks like there's no enchantment :(")
                .build();
          }


        })

        .setRunnable((user, context) -> {
          if (current == null) {
            return;
          }


        })
        .build();
  }

  public void selectRandomEnchantment() {
    final int maxAttempts = 1000;
    int attempts = 0;

    List<SellableEnchantment> sellable = config.enchantments;

    if (sellable.isEmpty()) {
      pickEnchantment(null, NONE, NONE);
      LOGGER.error("No enchantments that enchant merchant can choose from!");
      return;
    }

    Enchantment selected = null;
    SellLevel sellLevel = null;

    while (true) {
      attempts++;

      if (attempts >= maxAttempts) {
        LOGGER.warn("Failed to find valid enchantment in {} loops, using last selected",
            attempts
        );

        break;
      }

      SellableEnchantment item = sellable.get(random.nextInt(sellable.size()));
      Enchantment enchantment = item.enchantment;

      SellLevel level = item.pickLevel(random);
      if (level == null) {
        LOGGER.error("Enchantment {} has no 'level's value set, cannot use", enchantment.getKey());
        continue;
      }

      sellLevel = level;
      selected = enchantment;

      if (alreadyChosen.contains(enchantment.getKey())) {
        continue;
      }

      break;
    }

    if (selected == null) {
      LOGGER.error("Enchantment picker failed, no enchantment will be sold");
      pickEnchantment(null, NONE, NONE);

      return;
    }

    pickEnchantment(selected, sellLevel.level, sellLevel.price);

    LOGGER.info("Chose {} as the new enchantment, level={} price={}",
        selected.key(), sellLevel.level, sellLevel.price
    );
  }

  public void pickEnchantment(Enchantment enchantment, int level, int price) {
    this.current = enchantment;
    this.currentLevel = level;
    this.currentPrice = price;

    if (enchantment != null) {
      alreadyChosen.add(enchantment.getKey());
    }
  }

  @Override
  protected void saveDataTo(CompoundTag tag) {
    if (!alreadyChosen.isEmpty()) {
      ListTag list = BinaryTags.listTag();
      for (NamespacedKey key : alreadyChosen) {
        list.add(BinaryTags.stringTag(key.asString()));
      }
      tag.put(TAG_CHOSEN, list);
    }

    if (current != null) {
      CURRENT_CODEC.encodeStart(TagOps.OPS, current)
          .mapError(s -> "Failed to save " + TAG_CURRENT + ": " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(binaryTag -> tag.put(TAG_CURRENT, binaryTag));
    }
  }

  @Override
  protected void loadDataFrom(CompoundTag tag) {
    if (tag.contains(TAG_CURRENT, TagTypes.stringType())) {
      BinaryTag currentTag = tag.get(TAG_CURRENT);

      CURRENT_CODEC.parse(TagOps.OPS, currentTag)
          .mapError(s -> "Failed to load " + TAG_CURRENT + ": " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(enchantment -> this.current = enchantment);
    }

    ListTag list = tag.getList(TAG_CHOSEN, TagTypes.stringType());
    for (BinaryTag binaryTag : list) {
      String string = binaryTag.toString();
      NamespacedKey key = NamespacedKey.fromString(string);

      if (key == null) {
        LOGGER.warn("Invalid enchantment key found in enchant merchant data: {}", key);
        continue;
      }

      alreadyChosen.add(key);
    }
  }

  record Config(List<SellableEnchantment> enchantments, String menuTitle) {
    static final Config EMPTY = new Config(List.of(), "Enchantment Merchant");

    static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              SellableEnchantment.CODEC.listOf()
                  .fieldOf("enchantments")
                  .forGetter(Config::enchantments),

              ExtraCodecs.strictOptional(Codec.STRING, "menu-title", EMPTY.menuTitle)
                  .forGetter(Config::menuTitle)
          )
          .apply(instance, Config::new);
    });
  }

  record SellableEnchantment(Enchantment enchantment, List<SellLevel> levels) {
    static final Codec<SellableEnchantment> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.registryCodec(Registry.ENCHANTMENT)
                  .fieldOf("enchantment")
                  .forGetter(SellableEnchantment::enchantment),

              SellLevel.CODEC.listOf()
                  .fieldOf("levels")
                  .forGetter(SellableEnchantment::levels)
          )
          .apply(instance, SellableEnchantment::new);
    });

    public SellLevel pickLevel(Random random) {
      if (levels.isEmpty()) {
        return null;
      }
      if (levels.size() == 1) {
        return levels.get(0);
      }
      return levels.get(random.nextInt(levels.size()));
    }
  }

  record SellLevel(int level, int price) {
    static final Codec<SellLevel> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.INT.fieldOf("level").forGetter(SellLevel::level),
              Codec.INT.fieldOf("price").forGetter(SellLevel::price)
          )
          .apply(instance, SellLevel::new);
    });
  }
}
