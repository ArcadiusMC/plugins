package net.arcadiusmc.titles;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.currency.CurrencyMap;
import net.arcadiusmc.user.currency.CurrencyMaps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

@Getter
public class Title implements ComponentLike, ReloadableElement {

  /** The rank's tier */
  private final Tier tier;

  /** The rank's prefix without the trailing space */
  private final Component truncatedPrefix;

  /** The registry key of the opposite gender variant of this rank */
  @Pattern(Registries.VALID_KEY_REGEX)
  private final String genderEquivalentKey;

  /** This rank's menu slot, may be null */
  private final Slot menuSlot;

  /** Description text */
  private final ImmutableList<Component> description;

  /**
   * If true, means this rank will not be displayed until a user has been given
   * this rank
   */
  private final boolean hidden;

  /**
   * Determines if this rank can be reloaded, aka, if the user ranks are
   * reloaded, then this rank will be unregistered
   */
  private final boolean reloadable;

  @Setter
  private CurrencyMap<Integer> price = CurrencyMaps.emptyMap();

  public Title(
      Tier tier,
      Component truncatedPrefix,
      String genderEquivalentKey,
      Slot menuSlot,
      ImmutableList<Component> description,
      boolean hidden,
      boolean reloadable
  ) {
    this.tier = tier;
    this.truncatedPrefix = truncatedPrefix;
    this.genderEquivalentKey = genderEquivalentKey;
    this.menuSlot = menuSlot;
    this.description = description;
    this.hidden = hidden;
    this.reloadable = reloadable;
  }

  public Component getPrefix() {
    return getTruncatedPrefix().append(Component.space());
  }

  @Override
  public @NotNull Component asComponent() {
    return getTruncatedPrefix().hoverEvent(
        TextJoiner.onNewLine()
            .setColor(NamedTextColor.GRAY)
            .add(description)
            .asComponent()
    );
  }

  public Holder<Title> getGenderEquivalent() {
    return Strings.isNullOrEmpty(genderEquivalentKey)
        ? null
        : Titles.REGISTRY.getHolder(getGenderEquivalentKey()).orElse(null);
  }

  /* -------------------------- OBJECT OVERRIDES -------------------------- */

  @Override
  public int hashCode() {
    return Objects.hash(tier, getTruncatedPrefix());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Title rank)) {
      return false;
    }

    return Objects.equals(rank.getTier(), getTier())
        && Objects.equals(rank.getPrefix(), getPrefix());
  }
}