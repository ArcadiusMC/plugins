package net.arcadiusmc.titles;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.registry.Ref;
import net.arcadiusmc.registry.Ref.KeyRef;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.titles.events.TierPostChangeEvent;
import net.arcadiusmc.user.ComponentName;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserComponent;
import net.arcadiusmc.user.UserOfflineException;
import net.arcadiusmc.utils.TransformingSet;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Data and functions relating to tiers and titles a user can have.
 *
 * @see Title
 * @see Tier
 * @see #ensureSynced()
 */
@ComponentName("rankData")
public class UserTitles implements UserComponent {
  private static final Logger LOGGER = Loggers.getLogger();

  public static final String KEY_TITLE = "title";
  public static final String KEY_AVAILABLE = "titles";
  public static final String KEY_TIER = "tier";

  /* ----------------------------- INSTANCE FIELDS ------------------------------ */

  private final User user;

  /**
   * The user's currently active title
   */
  private KeyRef<Title> title = Titles.DEFAULT_REF;

  /**
   * The user's current tier
   */
  @Getter
  private Tier tier = Tiers.DEFAULT;

  /**
   * All non-default titles available to this user
   */
  private final TransformingSet<String, Title> available
      = Registries.keyBackedSet(Titles.REGISTRY);

  /* ----------------------------- CONSTRUCTORS ------------------------------ */

  public UserTitles(User user) {
    this.user = user;
  }

  /* ----------------------------- GETTERS ------------------------------ */

  /**
   * Gets the cloned set of all non-default titles this user has
   *
   * @return The user's non-default titles
   */
  public Set<Title> getAvailable() {
    return available;
  }

  public Title getTitle() {
    return title.orElse(Titles.REGISTRY, Titles.DEFAULT);
  }

  /* ----------------------------- TITLES ------------------------------ */

  /**
   * Tests if this user has the given title
   * <p>
   * If the given title is a 'default' title, then this will call {@link #hasTier(Tier)} with
   * the given title's tier.
   *
   * @param title The title to test for
   * @return True, if the user has this title
   * @see Title#isDefaultTitle()
   */
  public boolean hasTitle(Title title) {
    if (title.isDefaultTitle()) {
      return hasTier(title.getTier());
    }

    if (available.contains(title)) {
      return true;
    }

    var genderEquivalent = title.getGenderEquivalent();
    if (genderEquivalent == null) {
      return false;
    }

    return available.contains(genderEquivalent);
  }

  /**
   * Adds the given title to this user.
   * <p>
   * Delegate for: {@link #addTitle(Title, boolean)} with the boolean parameter as true
   *
   * @param title The title to add
   * @see #addTitle(Title, boolean)
   */
  public void addTitle(Title title) {
    addTitle(title, true);
  }

  /**
   * Adds the given title to this user and potentially changes the user's tier and permissions group
   * if the given title's tier is higher than the current tier.
   * <p>
   * The difference between the second and third parameters is that <code>givePermissions</code> is
   * passed onto {@link #setTier(Tier)} if the given title has a higher tier than the
   * user's current tier, and
   * <code>setTier</code> determines if the tier check should
   * be done at all.
   *
   * @param title           The title to add
   * @param setTier         True, to test if the user's tier should be changed if it's higher
   */
  public void addTitle(Title title, boolean setTier) {
    if (!title.isDefaultTitle()) {
      available.add(title);

      if (title.getGenderEquivalent() != null) {
        available.add(title.getGenderEquivalent());
      }
    }

    if (!hasTier(title.getTier()) && setTier) {
      setTier(title.getTier());
    }
  }

  /**
   * Removes the given title. If the title is a 'default' title, this method does nothing
   *
   * @param title The title to remove
   */
  public void removeTitle(Title title) {
    if (title.isDefaultTitle()) {
      return;
    }

    available.remove(title);

    if (title.getGenderEquivalent() != null) {
      available.remove(title.getGenderEquivalent());
    }

    //recalculateLoginEffect();
  }

  /**
   * Sets the user's active title.
   * <p>
   * If the user is online, this calls {@link User#updateTabName()}
   *
   * @param title The title to set
   */
  public void setTitle(Title title) {
    var titleKey = Titles.REGISTRY.getKey(title).orElseThrow();
    this.title = Ref.key(titleKey);

    if (!user.isOnline()) {
      return;
    }

    user.updateTabName();
  }

  /* ----------------------------- TIERS ------------------------------ */

  /**
   * Tests if the user's current tier is equal to or greater than the given tier
   *
   * @param tier The tier to test against
   * @return True, if the user's current tier is higher than or equal to the given tier
   */
  public boolean hasTier(Tier tier) {
    return this.tier.isGreaterThan(tier) || this.tier.getPriority() == tier.getPriority();
  }

  /**
   * Adds the given tier to this user.
   * <p>
   * This method works by testing the given tier with {@link #hasTier(Tier)}. If that returns
   * true, this function does nothing, else it calls {@link #setTier(Tier)}
   * <p>
   * Exists so that a user's tier is never accidentally assigned to a lower tier by accident and
   * makes the aforementioned possible without having to write this function manually everytime
   *
   * @param tier The tier to add
   * @see #hasTier(Tier)
   * @see #setTier(Tier)
   */
  public void addTier(Tier tier) {
    if (hasTier(tier)) {
      return;
    }

    setTier(tier);
  }

  /**
   * Sets this user's tier.
   * <p>
   * If <code>recalculatePermissions</code> is set to true then this method will remove the user's
   * current luck perms group, if it's not the default group, and add the user to the given tier's
   * group, granted it's not the default group.
   *
   * @param tier                   The tier to set
   */
  public void setTier(Tier tier) {
    Objects.requireNonNull(tier);

    if (this.tier == tier) {
      return;
    }

    if (getTier() != Tiers.DEFAULT && getTier().isPermissionSync()) {
      Commands.executeConsole("lp user %s parent remove %s",
          user.getName(), getTier().getPermissionGroup()
      );
    }

    if (tier != Tiers.DEFAULT && tier.isPermissionSync()) {
      Commands.executeConsole("lp user %s parent add %s",
          user.getName(), tier.getPermissionGroup()
      );
    }

    Title currentTitle = getTitle();
    if (currentTitle.getTier().isGreaterThan(tier)) {
      setTitle(Titles.DEFAULT);
    }

    // This is a sin, but it has to fire after the tier has been changed
    this.tier = tier;

    TierPostChangeEvent event = new TierPostChangeEvent(user, this.tier, tier);
    event.callEvent();
  }

  /**
   * Ensures the user's {@link Tier} is synced to the user's permission
   * group and that the user's permission group is synced to the user's tier.
   * <p>
   * If the user has a permission group of a higher tier than the one they
   * currently have, then the user's tier is changed. If the user has a tier
   * higher than their luck perms group, then their luck perms group is upgraded
   *
   * @throws UserOfflineException If the user is offline
   */
  public void ensureSynced() throws UserOfflineException {
    user.ensureOnline();

    ArrayList<Tier> values = new ArrayList<>(Tiers.REGISTRY.values());
    values.sort(Comparator.naturalOrder());

    for (Tier tier : values) {
      if (!tier.isPermissionSync()) {
        continue;
      }

      if (user.hasPermission("group." + tier.getPermissionGroup())) {
        if (hasTier(tier)) {
          continue;
        }

        LOGGER.info("Adding tier {} to {}", tier, user.getName());
        this.tier = tier;

        return;
      } else if (hasTier(tier)) {
        LOGGER.info("Adding group {} to {}, due to title/group sync",
            tier.getPermissionGroup(), user.getName()
        );

        Commands.executeConsole(
            "lp user %s parent add %s",
            user.getName(), tier.getPermissionGroup()
        );
        return;
      }
    }
  }

  /**
   * Clears all available titles, sets the active title to
   * {@link Titles#DEFAULT} and the current
   * tier to {@link Tiers#DEFAULT}
   */
  public void clear() {
    available.clear();
    title = Titles.DEFAULT_REF;
    tier = Tiers.DEFAULT;
  }

  /* ----------------------------- SERIALIZATION ------------------------------ */

  public void deserialize(@Nullable JsonElement element) {
    clear();

    if (element == null) {
      return;
    }

    var json = JsonWrapper.wrap(element.getAsJsonObject());

    if (json.has(KEY_TIER)) {
      tier = Tiers.REGISTRY.decode(JsonOps.INSTANCE, json.get(KEY_TIER))
          .mapError(s -> "Couldn't load tier " + s)
          .resultOrPartial(LOGGER::error)
          .orElse(Tiers.DEFAULT);
    } else {
      LOGGER.error("Missing 'tier' value, defaulting to 'none'");
      tier = Tiers.DEFAULT;
    }

    if (json.has(KEY_TITLE)) {
      this.title = Ref.key(json.getString(KEY_TITLE));
    }

    if (json.has(KEY_AVAILABLE)) {
      JsonArray arr = json.getArray(KEY_AVAILABLE);

      JsonUtils.stream(arr)
          .map(JsonElement::getAsString)
          .forEach(s -> available.getBackingSet().add(s));
    }
  }

  public @Nullable JsonElement serialize() {
    var json = JsonWrapper.create();

    if (tier != Tiers.DEFAULT) {
      Tiers.REGISTRY.encode(JsonOps.INSTANCE, tier)
          .mapError(s -> "Failed to save tier: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(element -> json.add(KEY_TIER, element));
    }

    if (!title.key().equalsIgnoreCase(Titles.DEFAULT_NAME)) {
      json.add(KEY_TITLE, title.key());
    }

    if (!available.isEmpty()) {
      JsonArray arr = JsonUtils.ofStream(
          available.getBackingSet()
              .stream()
              .map(JsonPrimitive::new)
      );

      if (!arr.isEmpty()) {
        json.add(KEY_AVAILABLE, arr);
      }
    }

    return json.nullIfEmpty();
  }
}