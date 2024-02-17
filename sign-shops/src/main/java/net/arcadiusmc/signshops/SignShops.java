package net.arcadiusmc.signshops;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.UUID;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.math.WorldVec3i;
import net.forthecrown.nbt.BinaryTags;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Sign shop constants and utility methods
 */
public final class SignShops {
  private SignShops() {}

  /* ----------------------------- CONSTANTS ------------------------------ */

  /**
   * The current key used to tell the shop manager which blocks are and aren't shops.
   * <p>
   * Also, the key the sign's data is saved under in the {@link Sign}'s
   * {@link PersistentDataContainer}
   */
  public static final NamespacedKey SHOP_KEY = new NamespacedKey("arcadiusmc", "signshop");

  public static final String RESELL_DISABLE_TAG = "resell_disabled";

  /**
   * The free item slot in the initial item selection screen
   */
  public static final int EXAMPLE_ITEM_SLOT = 2;

  /**
   * The default inventory size of shops
   */
  public static final int DEFAULT_INV_SIZE = 9;

  /**
   * The sign line that has the shop's type
   */
  public static final int LINE_TYPE = 0;

  /**
   * The sign line that has the shop's price
   */
  public static final int LINE_PRICE = 3;

  /**
   * The barrier item displayed in the Example Inventory
   */
  static final ItemStack EXAMPLE_BARRIER = ItemStacks.builder(Material.BARRIER, 1)
      .setNameRaw(Component.text(""))
      .build();

  /* ----------------------------- UTILITY METHODS ------------------------------ */

  public static Component priceLine(int price, Sign sign) {
    boolean usesCondensed = sign instanceof HangingSign;

    return Messages.render("signshops.labels.price", usesCondensed ? "hangingSign" : "regular")
        .addValue("price", price)
        .asComponent();
  }

  public static void makeNonResellable(ItemStack item) {
    item.editMeta(meta -> {
      ItemStacks.setTagElement(meta, RESELL_DISABLE_TAG, BinaryTags.byteTag(1));
    });
  }

  public static boolean isResellDisabled(ItemStack item) {
    return ItemStacks.hasTagElement(item.getItemMeta(), RESELL_DISABLE_TAG);
  }

  /**
   * Checks whether a block is a preexisting signshop. A null check is also performed in the
   * statement
   *
   * @param block The block to check
   * @return Whether the block is a shop or not
   */
  public static boolean isShop(Block block) {
    if (block == null) {
      return false;
    }

    if (block.getState() instanceof Sign sign) {
      return isShop(sign);
    } else {
      return false;
    }
  }

  public static boolean isShop(Sign sign) {
    PersistentDataContainer container = sign.getPersistentDataContainer();
    return container.has(SHOP_KEY, PersistentDataType.TAG_CONTAINER);
  }

  /**
   * Tests if the player with the given ID can edit the given shop.
   *
   * @param shop The shop to test the player against
   * @param uuid The player to test
   * @return True, if the player is allowed to edit the shop.
   */
  public static boolean mayEdit(SignShop shop, UUID uuid) {
    // Can't edit a shop that doesn't exist lol
    if (shop == null) {
      return false;
    }

    // If the UUID is that of the owner
    if (uuid.equals(shop.getOwner())) {
      return true;
    }

    // Get the shop's position and try to find a region that overlaps that area
    // If one is found, then check if it allows members to edit shops and then
    // check if the player's UUID is in the members list of the highest
    // priority region

    WorldVec3i vec = shop.getPosition();

    RegionManager manager = WorldGuard.getInstance()
        .getPlatform()
        .getRegionContainer()
        .get(BukkitAdapter.adapt(vec.getWorld()));

    if (manager == null) {
      return false;
    }

    BlockVector3 wePos = BlockVector3.at(vec.x(), vec.y(), vec.z());
    ApplicableRegionSet set = manager.getApplicableRegions(wePos);

    if (set.size() < 1) {
      return false;
    }

    ProtectedRegion first = set.iterator().next();
    State memberEditingAllowed = first.getFlag(SignShopFlags.MEMBER_EDITING);

    if (memberEditingAllowed != State.ALLOW) {
      return false;
    }

    return first.getMembers().contains(uuid) || first.getOwners().contains(uuid);
  }

  /**
   * Creates an inventory for the given {@link SignShop} instance.
   * <p>
   * Delegate method for {@link Bukkit#createInventory(InventoryHolder, int, Component)}
   *
   * @param shop The shop to create the inventory for
   * @param size The size of the inventory
   * @return The created inventory
   */
  public static Inventory createInventory(SignShop shop, int size) {
    return Bukkit.createInventory(shop, size, Component.text("Shop Contents"));
  }

  /**
   * Gets the hopper inventory with 1 available slot, used for setting the exampleItem of a shop
   *
   * @return the example inventory
   */
  public static Inventory createExampleInventory() {
    // Create example inventory
    Inventory inv = Bukkit.createInventory(
        null,
        InventoryType.HOPPER,
        Component.text("Specify what and how much")
    );

    // Fill these 4 slots, so they can only place
    // 1 item in the inventory.
    inv.setItem(0, EXAMPLE_BARRIER);
    inv.setItem(1, EXAMPLE_BARRIER);
    inv.setItem(3, EXAMPLE_BARRIER);
    inv.setItem(4, EXAMPLE_BARRIER);

    return inv;
  }
}