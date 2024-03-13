package net.arcadiusmc.usables;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Map;
import javax.annotation.Nullable;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.usables.objects.UsableBlock;
import net.arcadiusmc.usables.objects.UsableEntity;
import net.arcadiusmc.usables.objects.UsableItem;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public final class Usables {
  private Usables() {}

  public static final NamespacedKey BLOCK_KEY  = new NamespacedKey("arcadiusmc", "usable/block");
  public static final NamespacedKey ENTITY_KEY = new NamespacedKey("arcadiusmc", "usable/entity");
  public static final NamespacedKey ITEM_KEY   = new NamespacedKey("arcadiusmc", "usable/item");

  public static final int NO_FAILURE = -1;

  public static final TextReplacementConfig NEWLINES = TextReplacementConfig.builder()
      .matchLiteral("\\n")
      .replacement("\n")
      .build();

  public static boolean isUsable(Block block) {
    if (!(block.getState() instanceof TileState tile)) {
      return false;
    }

    return hasTag(tile, BLOCK_KEY);
  }

  public static boolean isUsable(Entity entity) {
    return hasTag(entity, ENTITY_KEY);
  }

  public static boolean isUsable(@Nullable ItemStack itemStack) {
    if (ItemStacks.isEmpty(itemStack)) {
      return false;
    }
    return hasTag(itemStack.getItemMeta(), ITEM_KEY);
  }

  private static boolean hasTag(PersistentDataHolder holder, NamespacedKey key) {
    return holder.getPersistentDataContainer().has(key, PersistentDataType.TAG_CONTAINER);
  }

  public static UsableEntity entity(Entity entity) {
    return new UsableEntity(entity);
  }

  public static UsableBlock block(Block block) {
    return new UsableBlock(block);
  }

  public static UsableItem item(ItemStack itemStack) {
    return new UsableItem(itemStack);
  }

  public static String boundsDisplay(IntRange ints) {
    if (ints == null || ints.isUnlimited()) {
      return "Any";
    }

    if (ints.isExact()) {
      return String.valueOf(ints.min().getAsInt());
    }

    if (ints.min().isEmpty()) {
      return "at most " + ints.max().getAsInt();
    }

    if (ints.max().isEmpty()) {
      return "at least " + ints.min().getAsInt();
    }

    return String.format("%s to %s", ints.min().getAsInt(), ints.max().getAsInt());
  }

  public static Component hoverableItemList(ItemList list) {
    Component itemList = TextJoiner.newJoiner()
        .setDelimiter(Component.text("\n- "))
        .setPrefix(Component.text("Items:\n"))
        .add(list.stream().map(Text::itemAndAmount))
        .asComponent();

    return Component.text("[Hover to see items]", NamedTextColor.AQUA).hoverEvent(itemList);
  }

  public static Component formatBaseString(String text, Audience viewer) {
    try {
      StringReader reader = new StringReader(text);
      return Arguments.CHAT.parse(reader).create(viewer);
    } catch (CommandSyntaxException exc) {
      return Text.valueOf(text, viewer);
    }
  }

  public static Component formatString(String str, Player viewer, Map<String, Object> context) {
    Component base = formatBaseString(str, viewer);
    PlaceholderRenderer list = Placeholders.newRenderer().useDefaults();

    return list.render(base, viewer, context)
        .replaceText(NEWLINES);
  }
}
