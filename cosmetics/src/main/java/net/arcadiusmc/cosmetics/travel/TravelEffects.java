package net.arcadiusmc.cosmetics.travel;

import static net.kyori.adventure.text.Component.text;

import net.arcadiusmc.cosmetics.Cosmetic;
import net.arcadiusmc.cosmetics.CosmeticType;
import net.arcadiusmc.cosmetics.CosmeticsConfig;
import net.arcadiusmc.cosmetics.TypeMenuCallback.Purchasable;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import org.bukkit.Material;

public class TravelEffects {

  private static final TravelEffect BEAM_EFFECT = new BeamTravelEffect();
  private static final TravelEffect HEART_EFFECT = new HeartTravelEffect();
  private static final TravelEffect PINK_ROCKET_EFFECT = new PinkRocketTravelEffect();
  private static final TravelEffect SMOKE_EFFECT = new SmokeTravelEffect();

  public static final Cosmetic<TravelEffect> BEAM = Cosmetic.builder(BEAM_EFFECT)
      .displayName(text("Beam"))
      .description(text("Beam me up, Scott!"))
      .permission("arcadius.cosmetics.travel.beam")
      .menuSlot(Slot.of(3, 1))
      .build();

  public static final Cosmetic<TravelEffect> HEART = Cosmetic.builder(HEART_EFFECT)
      .displayName(text("Hearts"))
      .description(text("Fly in a blaze of love <3"))
      .permission("arcadius.cosmetics.travel.heart")
      .menuSlot(Slot.of(2, 1))
      .build();

  public static final Cosmetic<TravelEffect> PINK_ROCKET = Cosmetic.builder(PINK_ROCKET_EFFECT)
      .displayName(text("Pink Rocket"))
      .description(text("Fly to the moon"))
      .description(text("with sprinkles :D"))
      .permission("arcadius.cosmetics.travel.pinkrocket")
      .menuSlot(Slot.of(1, 1))
      .build();

  public static final Cosmetic<TravelEffect> SMOKE = Cosmetic.builder(SMOKE_EFFECT)
      .displayName(text("Smoke"))
      .description(text("Hit that vape, yo"))
      .description(text("amirite kids..."))
      .menuSlot(Slot.of(4, 1))
      .permission("arcadius.cosmetics.travel.smoke")
      .build();

  public static CosmeticType<TravelEffect> type;

  public static CosmeticType<TravelEffect> createType(CosmeticsConfig cfg) {
    CosmeticType<TravelEffect> type = new CosmeticType<>();

    type.setMenuCallbacks(new Purchasable<>(cfg::travelEffectsPrice, "gems"));
    type.setMenuSlot(Slot.of(5, 2));
    type.setMenuSize(Menus.sizeFromRows(3));
    type.setMenuItem(Material.FEATHER);
    type.setMenuTitle(text("Travel Effects"));

    type.setName(text("Travel Effect"));
    type.description(text("Spice your travels with some effects :D"));

    type.register("beam", BEAM);
    type.register("heart", HEART);
    type.register("pink_rocket", PINK_ROCKET);
    type.register("smoke", SMOKE);

    TravelEffects.type = type;
    return type;
  }
}
