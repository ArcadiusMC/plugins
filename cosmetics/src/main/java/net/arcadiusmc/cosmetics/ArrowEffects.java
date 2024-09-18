package net.arcadiusmc.cosmetics;

import static net.kyori.adventure.text.Component.text;

import net.arcadiusmc.cosmetics.TypeMenuCallback.Purchasable;
import net.arcadiusmc.menu.Slot;
import org.bukkit.Material;
import org.bukkit.Particle;

public class ArrowEffects {

  public static final Cosmetic<Particle> FLAME = Cosmetic.builder(Particle.FLAME)
      .displayName(text("Flame"))
      .description(text("Works perfectly with flame arrows."))
      .permission("arcadius.cosmetics.arrow.flame")
      .menuSlot(Slot.of(10))
      .build();

  public static final Cosmetic<Particle> SNOWY = Cosmetic.builder(Particle.ITEM_SNOWBALL)
      .displayName(text("Snowy"))
      .description(text("To stay in the Christmas spirit."))
      .permission("arcadius.cosmetics.arrow.snowy")
      .menuSlot(Slot.of(11))
      .build();

  public static final Cosmetic<Particle> SNEEZE = Cosmetic.builder(Particle.SNEEZE)
      .displayName(text("Sneeze"))
      .description(text("Cover the place in juicy snot."))
      .permission("arcadius.cosmetics.arrow.sneeze")
      .menuSlot(Slot.of(12))
      .build();

  public static final Cosmetic<Particle> CUPIDS_ARROWS = Cosmetic.builder(Particle.HEART)
      .displayName(text("Cupid's Arrows"))
      .description(text("Time to do some matchmaking..."))
      .permission("arcadius.cosmetics.arrow.cupidsarrow")
      .menuSlot(Slot.of(13))
      .build();

  public static final Cosmetic<Particle> CUPIDS_TWIN = Cosmetic.builder(Particle.DAMAGE_INDICATOR)
      .displayName(text("Cupid's Evil Twin"))
      .description(text("Time to undo some matchmaking..."))
      .permission("arcadius.cosmetics.arrow.cupidstwin")
      .menuSlot(Slot.of(14))
      .build();

  public static final Cosmetic<Particle> STICKY_TRAIL = Cosmetic.builder(Particle.DRIPPING_HONEY)
      .displayName(text("Sticky Trail"))
      .description(text("For those who enjoy looking at the trail lol"))
      .permission("arcadius.cosmetics.arrow.stickytrail")
      .menuSlot(Slot.of(15))
      .build();

  public static final Cosmetic<Particle> SMOKE = Cosmetic.builder(Particle.CAMPFIRE_COSY_SMOKE)
      .displayName(text("Smoke"))
      .description(text("Pretend to be a cannon."))
      .permission("arcadius.cosmetics.arrow.smoke")
      .menuSlot(Slot.of(16))
      .build();

  public static final Cosmetic<Particle> SOULS = Cosmetic.builder(Particle.SOUL)
      .displayName(text("Souls"))
      .description(text("Scary souls escaping from your arrows"))
      .permission("arcadius.cosmetics.arrow.souls")
      .menuSlot(Slot.of(19))
      .build();

  public static final Cosmetic<Particle> FIREWORK = Cosmetic.builder(Particle.FIREWORK)
      .displayName(text("Firework"))
      .description(text("Almost as if you're using a crossbow"))
      .permission("arcadius.cosmetics.arrow.firework")
      .menuSlot(Slot.of(20))
      .build();

  public static CosmeticType<Particle> type;

  public static CosmeticType<Particle> createType(CosmeticsConfig cfg) {
    CosmeticType<Particle> type = new CosmeticType<>();

    type.setMenuItem(Material.BOW);
    type.setMenuCallbacks(new Purchasable<>(cfg::arrowEffectsPrice, "gems"));
    type.setMenuSlot(Slot.of(2, 1));
    type.setMenuTitle(text("Arrow Effects"));

    type.setName(text("Arrow Effect"));
    type.description(text("Upgrade your arrows with fancy particle"));
    type.description(text("trails as they fly through the air"));

    type.register("flame",             FLAME);
    type.register("snowy",             SNOWY);
    type.register("sneeze",            SNEEZE);
    type.register("cupids_arrows",     CUPIDS_ARROWS);
    type.register("cupids_evil_twin",  CUPIDS_TWIN);
    type.register("sticky_trail",      STICKY_TRAIL);
    type.register("smoke",             SMOKE);
    type.register("souls",             SOULS);
    type.register("firework",          FIREWORK);

    ArrowEffects.type = type;
    return type;
  }
}
