package net.arcadiusmc.titles;


import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.User;

@Data
@AllArgsConstructor
public class UserTitles {

  static final String TIER_PERMISSION_PREFIX = "arcadius.ranktier.";
  static final String TITLE_PERMISSION_PREFIX = "arcadius.rank.";

  private final User user;
  private final ActiveTitleMap map;

  private final List<Holder<Tier>> tiers;
  private final List<Holder<Title>> available;

  private Holder<Title> active;

  public static UserTitles load(User user) {
    TitlesPlugin plugin = TitlesPlugin.plugin();
    ActiveTitleMap map = plugin.getTitleMap();

    List<Holder<Title>> titles = new ArrayList<>();
    List<Holder<Tier>> tiers = new ArrayList<>();
    Holder<Title> active = map.getTitle(user.getUniqueId()).orElse(Titles.DEFAULT_HOLDER);

    for (Holder<Title> entry : Titles.REGISTRY.entries()) {
      String permission = getTitlePermission(entry);

      if (!Strings.isNullOrEmpty(permission) && !user.hasPermission(permission)) {
        continue;
      }

      titles.add(entry);
    }

    for (Holder<Tier> entry : Tiers.REGISTRY.entries()) {
      String permission = getTierPermission(entry);

      if (!Strings.isNullOrEmpty(permission) && !user.hasPermission(permission)) {
        continue;
      }

      tiers.add(entry);
    }

    if (tiers.isEmpty()) {
      tiers.add(Tiers.DEFAULT_HOLDER);
    }

    tiers.sort(Tiers.BY_PRIORITY);
    return new UserTitles(user, map, tiers, titles, active);
  }

  public static String getTitlePermission(Holder<Title> holder) {
    return TITLE_PERMISSION_PREFIX + holder.getKey();
  }

  public static String getTierPermission(Holder<Tier> holder) {
    String group = holder.getValue().getPermissionGroup();

    if (Strings.isNullOrEmpty(group)) {
      return TIER_PERMISSION_PREFIX + holder.getKey();
    }

    if (group.equals("none")) {
      return null;
    }

    return "group." + group;
  }

  public boolean hasTier(Tier tier) {
    for (Holder<Tier> tierHolder : tiers) {
      if (tierHolder.getValue().equals(tier)) {
        return true;
      }
    }

    return false;
  }

  public boolean hasTitle(Holder<Title> title) {
    Objects.requireNonNull(title, "Null title");

    Title value = title.getValue();
    Holder<Title> genderSwapped = value.getGenderEquivalent();

    return available.contains(title) || (genderSwapped != null && available.contains(genderSwapped));
  }

  public void setTitle(Holder<Title> holder) {
    map.setTitle(user.getUniqueId(), holder);
    active = holder;
    user.updateTabName();
  }

  public void addTitle(Holder<Title> holder) {
    if (available.contains(holder)) {
      return;
    }

    String permission = getTitlePermission(holder);

    if (Strings.isNullOrEmpty(permission)) {
      return;
    }

    user.setPermission(permission, true);
    available.add(holder);
  }

  public Holder<Tier> getTier() {
    if (tiers.isEmpty()) {
      return Tiers.DEFAULT_HOLDER;
    }

    return tiers.get(0);
  }

  public List<Holder<Tier>> getTiers() {
    return Collections.unmodifiableList(tiers);
  }

  public List<Holder<Title>> getAvailable() {
    return Collections.unmodifiableList(available);
  }
}