package net.arcadiusmc.utils;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.user.User;
import org.apache.commons.lang3.Range;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;

/**
 * Represents a set of tiered permissions.
 * <p>
 * Tiered permissions are a group of permissions considered to represent an integer value
 * in a predefined set of values. Basically this class acts as a wrapper for a set of permissions
 * that are mapped to integer values.
 */
public class TieredPermission {

  /**
   * The prefix used for permissions
   */
  @Getter
  private final String prefix;

  /**
   * The range of supported integer values for tiers
   */
  @Getter
  private final IntList tiers;

  private final PermissionTier[] permissions;

  /** Tier priority */
  @Getter
  private final TierPriority priority;

  /**
   * The unlimited permission, if a player has this permission then {@link Integer#MAX_VALUE} or
   * {@link Integer#MIN_VALUE} is returned, based on {@link #getPriority()}
   */
  @Getter
  private final Permission unlimitedPermission;

  private TieredPermission(Builder builder) {
    this.prefix = Objects.requireNonNull(builder.prefix);
    this.priority = Objects.requireNonNull(builder.priority);
    this.tiers = IntLists.unmodifiable(builder.tiers);

    Preconditions.checkArgument(!tiers.isEmpty(), "Empty tier list");

    this.permissions = new PermissionTier[tiers.size()];
    this.unlimitedPermission = builder.unlimitedPermission;

    int index = 0;
    for (int tier: tiers) {
      PermissionTier perm = new PermissionTier(
          tier,
          Permissions.register(prefix + tier)
      );

      permissions[index++] = perm;
    }

    Arrays.sort(permissions, getPriority());
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Get max tier's value
   * @return Max tier's value
   */
  public int getMaxTier() {
    return priority == TierPriority.HIGHEST
        ? permissions[0].tier()
        : permissions[permissions.length - 1].tier();
  }

  /**
   * Get min tier's value
   * @return Min tier's value
   */
  public int getMinTier() {
    return priority == TierPriority.LOWEST
        ? permissions[0].tier()
        : permissions[permissions.length - 1].tier();
  }

  /**
   * Gets the tier value of the specified {@code user}.
   * <p>
   * If the specified has none of permissions that belong to a tier set, an empty optional
   * is returned.
   * <p>
   * If an {@link #getUnlimitedPermission()} has been set, and the {@code player} has the
   * permission, then either {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} is
   * returned, based on {@link #getPriority()}.
   *
   * @param user Player
   * @return Player's tier, or empty, if the player has none of the tier set's permissions
   */
  public OptionalInt getTier(User user) {
    return _getTier(user::hasPermission);
  }

  /**
   * Gets the tier value of the specified {@code permissible}.
   * <p>
   * If the specified has none of permissions that belong to a tier set, an empty optional
   * is returned.
   * <p>
   * If an {@link #getUnlimitedPermission()} has been set, and the {@code permissible} has the
   * permission, then either {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} is
   * returned, based on {@link #getPriority()}.
   *
   * @param permissible Player
   * @return Player's tier, or empty, if the player has none of the tier set's permissions
   */
  public OptionalInt getTier(Permissible permissible) {
    return _getTier(permissible::hasPermission);
  }

  /**
   * Tests if the specified {@code permissible} has this tier set's unlimited permission.
   * <p>
   * If no {@link #getUnlimitedPermission()} has been set, this will always return false.
   *
   * @param permissible Player
   * @return {@code true}, if an unlimited permission has been defined and the player has,
   *         {@code false} otherwise
   */
  public boolean hasUnlimited(Permissible permissible) {
    return unlimitedPermission != null && permissible.hasPermission(unlimitedPermission);
  }

  /**
   * Tests if the specified {@code user} has this tier set's unlimited permission.
   * <p>
   * If no {@link #getUnlimitedPermission()} has been set, this will always return false.
   *
   * @param user Player
   * @return {@code true}, if an unlimited permission has been defined and the player has,
   *         {@code false} otherwise
   */
  public boolean hasUnlimited(User user) {
    return unlimitedPermission != null && user.hasPermission(unlimitedPermission);
  }

  private OptionalInt _getTier(Predicate<Permission> holder) {
    if (unlimitedPermission != null && holder.test(unlimitedPermission)) {
      return OptionalInt.of(
          priority == TierPriority.HIGHEST
              ? Integer.MAX_VALUE
              : Integer.MIN_VALUE
      );
    }

    for (var t: permissions) {
      if (holder.test(t.permission)) {
        return OptionalInt.of(t.tier);
      }
    }

    return OptionalInt.empty();
  }

  /**
   * Tests if this tier set contains the specified tier value.
   * @param tier Tier
   * @return {@code true}, if this object contains a permission mapped to the specified {@code tier},
   *         {@code false} otherwise.
   */
  public boolean contains(int tier) {
    return getPermission(tier) != null;
  }

  /**
   * Gets the permission mapped to the specified {@code tier} value.
   *
   * @param tier Tier
   * @return Tier mapped permission, or {@code null}, if no value mapped to the specified
   *         tier exists.
   */
  public Permission getPermission(int tier) {
    for (var t: permissions) {
      if (t.tier == tier) {
        return t.permission;
      }
    }

    return null;
  }

  public enum TierPriority implements Comparator<PermissionTier> {
    HIGHEST {
      @Override
      public int compare(PermissionTier o1, PermissionTier o2) {
        return Integer.compare(o2.tier, o1.tier);
      }
    },

    LOWEST {
      @Override
      public int compare(PermissionTier o1, PermissionTier o2) {
        return Integer.compare(o1.tier, o2.tier);
      }
    }
  }

  private record PermissionTier(int tier, Permission permission) {}

  @Getter
  @Setter
  @Accessors(chain = true, fluent = true)
  public static class Builder {

    private String prefix;
    private Permission unlimitedPermission;

    private TierPriority priority = TierPriority.HIGHEST;

    private final IntList tiers = new IntArrayList();

    public Builder tiersFrom1To(int end) {
      return range(Range.between(1, end));
    }

    public Builder tiersBetween(int min, int max) {
      return range(Range.between(min, max));
    }

    public Builder range(Range<Integer> range) {
      tiers.clear();
      for (int i = range.getMinimum(); i <= range.getMaximum(); i++) {
        tiers.add(i);
      }

      return this;
    }

    public Builder tiers(int... tiers) {
      this.tiers.addAll(IntList.of(tiers));
      return this;
    }

    public Builder addTier(int tier) {
      tiers.add(tier);
      return this;
    }

    public Builder unlimitedPerm(String perm) {
      return unlimitedPermission(Permissions.register(perm));
    }

    public Builder allowUnlimited() {
      return unlimitedPerm(prefix + (prefix.endsWith(".") ? "" : ".") + "unlimited");
    }

    public TieredPermission build() {
      return new TieredPermission(this);
    }
  }
}