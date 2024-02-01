package net.arcadiusmc.titles;

import static net.arcadiusmc.titles.RankTier.NONE;
import static net.arcadiusmc.titles.TitleSettings.SEE_RANKS;

import com.google.gson.JsonElement;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Ref;
import net.arcadiusmc.registry.Ref.KeyRef;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.DisplayIntent;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;

@SuppressWarnings("unused")
public final class UserRanks {
  private UserRanks() {}

  public static final Registry<UserRank> REGISTRY = Registries.newRegistry();

  static {
    REGISTRY.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<UserRank> value) {
        UserRank title = value.getValue();
        RankTier tier = title.getTier();
        tier.titles.add(title);
      }

      @Override
      public void onUnregister(Holder<UserRank> value) {
        UserRank title = value.getValue();
        RankTier tier = title.getTier();
        tier.titles.remove(title);
      }
    });
  }

  public static final String DEFAULT_NAME = "default";

  public static final KeyRef<UserRank> DEFAULT_REF = Ref.key(DEFAULT_NAME);

  public static final UserRank DEFAULT = UserRank.builder()
      .slot(1, 1)
      .defaultTitle(true)
      .prefix("No Title")
      .hidden(true)
      .tier(NONE)
      .registered(DEFAULT_NAME);

  /**
   * Removes all dynamically loaded ranks from the rank registry.
   * <p>
   * Whether a rank is dynamically loaded is tested with {@link UserRank#isReloadable()}
   */
  public static void clearNonConstants() {
    REGISTRY.removeIf(h -> h.getValue().isReloadable());
  }

  public static boolean showRank(DisplayContext context) {
    // Don't display rank prefix if the user has disabled it,
    // only in certain circumstances though
    return !context.intentMatches(DisplayIntent.UNSET, DisplayIntent.HOVER_TEXT)
         || context.viewerProperty(SEE_RANKS);
  }

  public static Result<UserRank> deserialize(JsonElement element) {
    if (element == null || !element.isJsonObject()) {
      return Result.error("Invalid JSON: " + element);
    }

    JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());
    var prefix = json.getComponent("prefix");

    if (prefix == null) {
      return Result.error("No prefix set");
    }

    var builder = UserRank.builder()
        .reloadable(true)
        .truncatedPrefix(prefix)
        .genderEquivalentKey(json.getString("genderEquivalent"))
        .hidden(json.getBool("hidden", false))
        .defaultTitle(json.getBool("defaultTitle", false));

    if (json.has("tier")) {
      builder.tier(json.getEnum("tier", RankTier.class));

      if (builder.tier() == NONE) {
        return Result.error("Tier NONE is not supported for ranks");
      }
    } else {
      return Result.error("No tier set");
    }

    json.getList("description", JsonUtils::readText)
        .forEach(builder::addDesc);

    return Result.success(
        builder.menuSlot(json.get("slot", Slot::load))
            .build()
    );
  }
}