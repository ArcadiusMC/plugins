package net.arcadiusmc.cosmetics.command;

import static net.kyori.adventure.text.Component.text;

import net.arcadiusmc.cosmetics.Cosmetic;
import net.arcadiusmc.cosmetics.CosmeticType;
import net.arcadiusmc.cosmetics.MenuCallbacks;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import org.bukkit.Material;

public final class Emotes {
  private Emotes() {}

  public static final Cosmetic<Emote> BONK = create(new BonkEmote())
      .menuSlot(Slot.of(12))
      .build();

  public static final Cosmetic<Emote> SMOOCH = create(new KissEmote())
      .menuSlot(Slot.of(13))
      .build();

  public static final Cosmetic<Emote> POKE = create(new PokeEmote())
      .menuSlot(Slot.of(14))
      .build();

  public static final Cosmetic<Emote> SCARE = create(new ScareEmote())
      .menuSlot(Slot.of(21))
      .build();

  public static final Cosmetic<Emote> JINGLE = create(new JingleEmote())
      .menuSlot(Slot.of(22))
      .build();

  public static final Cosmetic<Emote> HUG = create(new HugEmote())
      .menuSlot(Slot.of(23))
      .build();

  public static CosmeticType<Emote> createType() {
    CosmeticType<Emote> type = new CosmeticType<>();
    type.setMenuTitle(text("Emotes"));
    type.setMenuItem(Material.GLOWSTONE_DUST);
    type.setMenuSize(Menus.sizeFromRows(4));
    type.setMenuSlot(Slot.of(3, 2));
    type.setMenuCallbacks(MenuCallbacks.nop());

    type.setName(text("Emote"));
    type.description(text("Poking, smooching, bonking and more"));
    type.description(text("to interact with friends!"));

    type.register("bonk", BONK);
    type.register("smooch", SMOOCH);
    type.register("poke", POKE);
    type.register("scare", SCARE);
    type.register("jingle", JINGLE);
    type.register("hug", HUG);

    return type;
  }

  private static Cosmetic.CosmeticBuilder<Emote> create(Emote emote) {
    return Cosmetic.builder(emote)
        .displayName(text("/" + emote.getName()))
        .description(emote.getCommand().description())
        .permission(emote.getPermission());
  }
}
