package net.arcadiusmc.merchants;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.menu.Menu;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.merchants.commands.CommandParrots;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.inventory.SkullItemBuilder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Parrot.Variant;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.slf4j.Logger;

public class ParrotMerchant extends Merchant {

  public static final String SCOREBOARD_TAG = "arcadius_merchant_parrot";

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<Map<UUID, List<Variant>>> OWNED_CODEC
      = Codec.unboundedMap(
          ExtraCodecs.STRING_UUID,

          ExtraCodecs.enumCodec(Variant.class).listOf()
              .xmap(ArrayList::new, ArrayList::new)
  );

  public static final ContextSet SET = ContextSet.create();
  public static final ContextOption<Boolean> PURCHASE_ALLOWED = SET.newOption(true);

  private Config config = Config.EMPTY;

  private final Map<UUID, List<Variant>> owned = new HashMap<>();

  @Getter
  private Menu menu;

  public ParrotMerchant(MerchantsPlugin plugin) {
    super(plugin, "parrots");
  }

  @Override
  protected void onEnable() {
    new CommandParrots(this);
  }

  @Override
  protected void clearData() {
    owned.clear();
  }

  @Override
  public void reloadConfig() {
    loadConfig(Config.CODEC, cfg -> this.config = cfg);
    createMenu();
  }

  @Override
  protected void loadDataFrom(CompoundTag tag) {
    OWNED_CODEC.parse(TagOps.OPS, tag)
        .mapError(s -> "Failed to load parrot data: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(map -> {
          map.forEach((uuid, variants) -> {
            owned.put(uuid, new ArrayList<>(variants));
          });
        });
  }

  @Override
  protected void saveDataTo(CompoundTag tag) {
    OWNED_CODEC.encodeStart(TagOps.OPS, owned)
        .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
        .mapError(s -> "Failed to save parrot data: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(tag::merge);
  }

  private void createMenu() {
    MenuBuilder builder = Menus.builder(Menus.sizeFromRows(3), "Parrots Shop")
        .setTitle(config.menuTitle)
        .add(MenuPage.HEADER_SLOT, MenuNode.builder().setItem(config.header).build())
        .addBorder();

    List<SellableParrot> parrots = config.parrots;

    for (int i = 0; i < parrots.size(); i++) {
      SellableParrot parrot = parrots.get(i);
      builder.add(Slot.of(i + 1, 1), parrotNode(parrot));
    }

    menu = builder.build();
  }

  @SuppressWarnings("deprecation")
  private MenuNode parrotNode(SellableParrot parrot) {
    return MenuNode.builder()
        .setItem((user, context) -> {
          SkullItemBuilder builder = ItemStacks.headBuilder();
          PlayerProfile profile = ItemStacks.profileFromTextureId(parrot.textureId);

          builder.editMeta(SkullMeta.class, skullMeta -> {
            skullMeta.setPlayerProfile(profile);
          });

          boolean owned = hasVariant(user, parrot.variant);

          if (owned) {
            Variant active = getVariant(user);

            if (active == parrot.variant) {
              builder.addLore(Messages.renderText("merchants.parrots.active"));
            } else {
              builder.addLore(Messages.renderText("merchants.parrots.ownedLore"));
            }
          } else {
            int price = parrot.price;
            builder.setName(text(parrot.name, NamedTextColor.YELLOW))
                .addLore(
                    Messages.render("merchants.priceLine")
                        .addValue("price", Messages.currency(price))
                        .create(user)
                )
                .addLore("");

            if (user.hasBalance(price)) {
              builder.addLore(Messages.renderText("merchants.clickToPurchase"));
            } else {
              builder.addLore(Messages.renderText("merchants.cannotAfford"));
            }
          }

          return builder.build();
        })

        .setRunnable((user, context, click) -> {
          int price = parrot.price();
          boolean owned = hasVariant(user, parrot.variant);

          if (!owned) {
            boolean allowed = context.get(PURCHASE_ALLOWED);

            if (!allowed) {
              throw Messages.render("merchants.parrots.purchaseNotAllowed")
                  .exception(user);
            }

            if (!user.hasBalance(price)) {
              throw Exceptions.cannotAfford(user, price);
            }

            List<Variant> list = this.owned.computeIfAbsent(user.getUniqueId(), uuid -> new ArrayList<>());
            list.add(parrot.variant);

            user.removeBalance(price);

            NamedTextColor color = switch (parrot.variant) {
              case GREEN -> NamedTextColor.GREEN;
              case GRAY -> NamedTextColor.GRAY;
              case CYAN -> NamedTextColor.AQUA;
              case BLUE -> NamedTextColor.BLUE;
              case RED -> NamedTextColor.RED;
            };

            user.sendMessage(
                Messages.render("merchants.parrots.bought")
                    .addValue("parrot", text(parrot.name, color))
                    .addValue("price", Messages.currency(price))
                    .create(user)
            );
          }

          Player player = user.getPlayer();
          Entity leftShoulder = player.getShoulderEntityLeft();

          if (leftShoulder == null) {
            setActive(user, parrot.variant);
            click.shouldReloadMenu(true);
            return;
          }

          boolean merchantParrot = leftShoulder.getScoreboardTags().contains(SCOREBOARD_TAG);

          if (!merchantParrot || !(leftShoulder instanceof Parrot parrot1)) {
            player.releaseLeftShoulderEntity();
            setActive(user, parrot.variant);
          } else {
            Variant variant = parrot1.getVariant();
            if (variant == parrot.variant) {
              player.setShoulderEntityLeft(null);
            } else {
              setActive(user, parrot.variant);
            }
          }

          click.shouldReloadMenu(true);
        })

        .build();
  }

  private void setActive(User user, Variant variant) {
    Location location = user.getLocation();
    World world = location.getWorld();

    Parrot spawned = world.spawn(location, Parrot.class, parrot -> {
      parrot.setAI(false);
      parrot.setPersistent(false);
      parrot.setVariant(variant);
      parrot.addScoreboardTag(SCOREBOARD_TAG);
    });

    user.getPlayer().setShoulderEntityLeft(spawned);
  }

  private Variant getVariant(User user) {
    Player player = user.getPlayer();
    Entity left = player.getShoulderEntityLeft();

    if (!(left instanceof Parrot parrot)) {
      return null;
    }

    Set<String> tags = parrot.getScoreboardTags();
    if (!tags.contains(SCOREBOARD_TAG)) {
      return null;
    }

    return parrot.getVariant();
  }

  private boolean hasVariant(User user, Variant variant) {
    List<Variant> list = owned.get(user.getUniqueId());

    if (list == null) {
      return false;
    }

    return list.contains(variant);
  }

  private record Config(List<SellableParrot> parrots, Component menuTitle, HeaderItem header) {
    static final Config EMPTY = new Config(List.of(), empty(), HeaderItem.DEFAULT);

    static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.strictOptional(SellableParrot.CODEC.listOf(), "parrots", List.of())
                  .forGetter(Config::parrots),

              ExtraCodecs.COMPONENT.fieldOf("menu-title")
                  .forGetter(Config::menuTitle),

              HeaderItem.CODEC.fieldOf("header")
                  .forGetter(Config::header)
          )
          .apply(instance, Config::new);
    });
  }

  private record SellableParrot(
      Variant variant,
      int price,
      String textureId,
      String name
  ) {
    static final Codec<SellableParrot> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.enumCodec(Variant.class)
                  .fieldOf("variant")
                  .forGetter(SellableParrot::variant),

              Codec.INT.fieldOf("price")
                  .forGetter(SellableParrot::price),

              Codec.STRING.fieldOf("texture-id")
                  .forGetter(SellableParrot::textureId),

              Codec.STRING.fieldOf("name")
                  .forGetter(SellableParrot::name)
          )
          .apply(instance, SellableParrot::new);
    });
  }
}
