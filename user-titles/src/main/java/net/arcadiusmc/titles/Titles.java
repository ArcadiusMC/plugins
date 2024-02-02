package net.arcadiusmc.titles;

import static net.arcadiusmc.titles.TitleSettings.SEE_RANKS;

import com.google.common.collect.ImmutableList;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Ref;
import net.arcadiusmc.registry.Ref.KeyRef;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.DisplayIntent;
import net.kyori.adventure.text.Component;

@SuppressWarnings("unused")
public final class Titles {
  private Titles() {}

  public static final Registry<Title> REGISTRY = Registries.newRegistry();

  static {
    REGISTRY.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<Title> value) {
        Title title = value.getValue();
        Tier tier = title.getTier();
        tier.getRanks().add(title);
      }

      @Override
      public void onUnregister(Holder<Title> value) {
        Title title = value.getValue();
        Tier tier = title.getTier();
        tier.getRanks().remove(title);
      }
    });
  }

  public static final String DEFAULT_NAME = "default";

  public static final KeyRef<Title> DEFAULT_REF = Ref.key(DEFAULT_NAME);

  public static final Title DEFAULT = new Title(
      Tiers.DEFAULT,
      Component.text("No Title"),
      null,
      Slot.of(1, 1),
      ImmutableList.of(),
      true,
      true,
      false
  );

  static {
    REGISTRY.register(DEFAULT_NAME, DEFAULT);
  }

  public static boolean showRank(DisplayContext context) {
    // Don't display rank prefix if the user has disabled it,
    // only in certain circumstances though
    return !context.intentMatches(DisplayIntent.UNSET, DisplayIntent.HOVER_TEXT)
         || context.viewerProperty(SEE_RANKS);
  }
}