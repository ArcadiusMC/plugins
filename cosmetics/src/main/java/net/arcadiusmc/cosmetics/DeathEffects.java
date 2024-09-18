package net.arcadiusmc.cosmetics;

import static net.kyori.adventure.text.Component.text;

import net.arcadiusmc.cosmetics.TypeMenuCallback.Purchasable;
import net.arcadiusmc.menu.Slot;
import org.bukkit.Material;

public class DeathEffects {

  public static final Cosmetic<DeathEffect> ENDER_RING = Cosmetic.builder(DeathEffect.ENDER_RING)
      .displayName(text("Ender Ring"))
      .description(text("Ender particles doing ring stuff."))
      .description(text("Makes you scream like an Enderman."))
      .permission("arcadius.cosmetics.death.enderring")
      .menuSlot(Slot.of(14))
      .build();

  public static final Cosmetic<DeathEffect> EXPLOSION = Cosmetic.builder(DeathEffect.EXPLOSION)
      .displayName(text("Creeper"))
      .description(text("Explode like a creeper."))
      .description(text("Always wanted to know what that feels like..."))
      .permission("arcadius.cosmetics.death.creeper")
      .menuSlot(Slot.of(15))
      .build();

  public static final Cosmetic<DeathEffect> SOUL_DEATH = Cosmetic.builder(DeathEffect.SOUL_DEATH)
      .displayName(text("Souls"))
      .description(text("The souls of your victims escaping"))
      .description(text("your body."))
      .permission("arcadius.cosmetics.death.souldeath")
      .menuSlot(Slot.of(11))
      .build();

  public static final Cosmetic<DeathEffect> TOTEM = Cosmetic.builder(DeathEffect.TOTEM)
      .displayName(text("Faulty Totem"))
      .description(text("The particles are there, but you still die?"))
      .permission("arcadius.cosmetics.death.totem")
      .menuSlot(Slot.of(12))
      .build();

  public static CosmeticType<DeathEffect> type;

  public static CosmeticType<DeathEffect> createType(CosmeticsConfig cfg) {
    CosmeticType<DeathEffect> type = new CosmeticType<>();

    type.setMenuCallbacks(new Purchasable<>(cfg::deathEffectsPrice, "gems"));
    type.setMenuSlot(Slot.of(6, 1));
    type.setMenuItem(Material.SKELETON_SKULL);
    type.setMenuTitle(text("Death Effects"));

    type.setName(text("Death Effect"));
    type.description(text("Make your death more spectacular by"));
    type.description(text("exploding into pretty particles"));

    type.register("ender_ring", ENDER_RING);
    type.register("explosion", EXPLOSION);
    type.register("soul_death", SOUL_DEATH);
    type.register("totem", TOTEM);

    DeathEffects.type = type;
    return type;
  }
}
