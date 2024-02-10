package net.arcadiusmc.titles;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class Tier implements ReloadableElement, Comparable<Tier> {

  private final Component displayName;
  private final Material displayItem;
  private final ImmutableList<Component> description;
  private final String permissionGroup;
  private final int priority;
  private final List<MenuDecoration> decorations;
  private final boolean permissionSync;

  private final boolean reloadable;

  private final List<Title> ranks = new ArrayList<>();

  private MenuNode cachedNode;
  private Component cachedDisplayName;

  @Setter
  private MenuPage page;

  public Component displayName() {
    if (cachedDisplayName != null) {
      return cachedDisplayName;
    }

    cachedDisplayName = displayName
        .colorIfAbsent(NamedTextColor.AQUA)
        .hoverEvent(TextJoiner.onNewLine().add(description).asComponent());

    return cachedDisplayName;
  }

  public MenuNode getNode() {
    if (cachedNode != null) {
      return cachedNode;
    }

    return cachedNode = MenuNode.builder()
        .setItem((user, context) -> {
          return ItemStacks.builder(displayItem)
              .addLore(description)
              .setName(displayName)
              .build();
        })
        .build();
  }

  public boolean isLesserThan(Tier other) {
    return other.priority > this.priority;
  }

  public boolean isGreaterThan(Tier other) {
    return other.priority < this.priority;
  }

  @Override
  public int compareTo(@NotNull Tier o) {
    // The parameters would normally be the other way around,
    // but Tiers are ordered by highest to lowest priority
    return Integer.compare(o.priority, this.priority);
  }

  @Override
  public String toString() {
    return "Tier[" + Text.plain(displayName) + "]";
  }

  public record MenuDecoration(Slot slot, Material material) {

  }
}
