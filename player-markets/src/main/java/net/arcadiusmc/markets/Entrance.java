package net.arcadiusmc.markets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.math.Vectors;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public record Entrance(Vector3i signPosition, Vector3d entityPosition, BlockFace direction) {

  public static final String ENTITY_TAG = "market_entrance_entity";
  public static final NamespacedKey KEY = new NamespacedKey("arcadiusmc", "markets/entrance");

  public static final PlayerProfile HEAD_PROFILE = createTextureProfile();

  private static final float VIEW_RANGE = 0.1f;

  static final Codec<Entrance> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Vectors.V3I_CODEC.fieldOf("sign_position")
                .forGetter(Entrance::signPosition),
            Vectors.V3D_CODEC.fieldOf("entity_position")
                .forGetter(Entrance::entityPosition),
            ExtraCodecs.enumCodec(BlockFace.class).fieldOf("direction")
                .forGetter(Entrance::direction)
        )
        .apply(instance, Entrance::new);
  });

  private static PlayerProfile createTextureProfile() {
    String textureLink = "http://textures.minecraft.net/texture/"
        + "7d16ae951120394f368f2250b7c3ad3fb12cea55ec1b2db5a94d1fb7fd4b6fa";

    PlayerProfile profile = Bukkit.getServer().createProfile(Identity.nil().uuid(), "Pearl");
    PlayerTextures textures = profile.getTextures();

    try {
      textures.setSkin(new URL(textureLink));
    } catch (MalformedURLException exc) {
      Loggers.getLogger().error("Couldn't set textures of profile", exc);
    }

    profile.setTextures(textures);

    if (!profile.hasTextures()) {
      CompletableFuture.runAsync(() -> {
        profile.complete(true);
      });
    }

    return profile;
  }

  public void onClaim(User newOwner, World world) {
    setSign(world, side -> {
      for (int i = 0; i < 4; i++) {
        Component line = Messages.render("markets.entrances.claimed.line" + (i + 1))
            .addValue("player", newOwner.getNickOrName())
            .asComponent();

        side.line(i, line);
      }
    });

    killEntities(world);
  }

  public void onUnclaim(World world, Market market) {
    setSign(world, side -> {
      for (int i = 0; i < 4; i++) {
        Component line = Messages.renderText("markets.entrances.unclaimed.line" + (i + 1));
        side.line(i, line);
      }
    });

    killEntities(world);

    Location location = getEntityLocation(world);

    world.spawn(location, Interaction.class, i -> {
      i.setInteractionHeight(1);
      i.setInteractionWidth(1);
      i.addScoreboardTag(ENTITY_TAG);

      PersistentDataContainer pdc = i.getPersistentDataContainer();
      pdc.set(KEY, PersistentDataType.STRING, market.getRegionName());
    });

    world.spawn(location, TextDisplay.class, display -> {
      display.setBillboard(Billboard.VERTICAL);
      display.setViewRange(VIEW_RANGE);
      display.addScoreboardTag(ENTITY_TAG);

      Transformation transformation = new Transformation(
          new Vector3f(0, 0.75f, 0),
          new AxisAngle4f(),
          new Vector3f(1, 1, 1),
          new AxisAngle4f()
      );

      display.setTransformation(transformation);
      display.text(Messages.renderText("markets.entrances.purchasePrompt"));
    });

    world.spawn(location, ItemDisplay.class, display -> {
      ItemStack item = ItemStacks.headBuilder()
          .setProfile(HEAD_PROFILE)
          .build();

      display.setItemStack(item);
      display.addScoreboardTag(ENTITY_TAG);

      Transformation transformation = new Transformation(
          new Vector3f(0, 0.5f, 0),
          new AxisAngle4f(),
          new Vector3f(1, 1, 1),
          new AxisAngle4f()
      );
      display.setTransformation(transformation);
      display.setViewRange(VIEW_RANGE);
    });
  }

  public void killEntities(World world) {
    Location location = getEntityLocation(world);

    Chunk chunk = location.getChunk();
    Entity[] entities = chunk.getEntities();

    double maxDistSq = Math.pow(2, 2);

    for (Entity entity : entities) {
      Location entityLocation = entity.getLocation();

      if (entityLocation.distanceSquared(location) > maxDistSq) {
        continue;
      }

      if (!entity.getScoreboardTags().contains(ENTITY_TAG)) {
        continue;
      }

      entity.remove();
    }
  }

  public Component display() {
    return Messages.render("markets.entrance.info")
        .addValue("sign", signPosition)
        .addValue("facing", direction)
        .addValue("entity", entityPosition)
        .asComponent();
  }

  Location getEntityLocation(World world) {
    return new Location(world, entityPosition.x(), entityPosition.y(), entityPosition.z());
  }

  void setSign(World world, Consumer<SignSide> consumer) {
    Block block = Vectors.getBlock(signPosition, world);

    org.bukkit.block.data.type.WallSign signData =
        (org.bukkit.block.data.type.WallSign) Material.OAK_WALL_SIGN.createBlockData();

    signData.setFacing(direction);
    block.setBlockData(signData, false);

    Sign sign = (Sign) block.getState();
    SignSide signSide = sign.getSide(Side.FRONT);
    signSide.setGlowingText(true);
    signSide.setColor(DyeColor.GRAY);

    consumer.accept(signSide);

    sign.update();
  }

  public void remove(World world) {
    killEntities(world);
    Block sign = Vectors.getBlock(signPosition, world);

    if (sign.getBlockData() instanceof org.bukkit.block.data.type.Sign) {
      sign.setType(Material.AIR, false);
    }
  }
}
