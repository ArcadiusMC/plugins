package net.arcadiusmc.core;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

public class Coinpile {

  public static final int MODEL_LARGE = 10090010;
  public static final int MODEL_MEDIUM = 10090009;
  public static final int MODEL_SMALL = 10090008;

  public static final float Y_TRANSFORM = 0.5f;
  public static final float INTERACTION_WIDTH = 0.9f;
  public static final float INTERACTION_HEIGHT = 0.5f;

  public static final NamespacedKey WORTH = NamespacedKey.fromString("arcadiusmc:coinpile_worth");
  public static final String SCOREBOARD_TAG = "coinpile";

  public static void create(Location location, int amount, int model) {
    World world = location.getWorld();
    if (world == null) {
      return;
    }

    location = location.clone();
    location.setPitch(0);
    location.setYaw((float) ((Math.random() * 180.0f) - 90.0f));

    Interaction interaction = world.spawn(location, Interaction.class);
    ItemDisplay display = world.spawn(location, ItemDisplay.class);

    ItemStack item = new ItemStack(Material.BAMBOO_BUTTON);
    item.editMeta(meta -> {
      meta.setCustomModelData(checkedModel(model));
    });

    display.setItemStack(item);

    Transformation transformation = display.getTransformation();
    transformation.getTranslation().y = Y_TRANSFORM;
    display.setTransformation(transformation);

    interaction.setInteractionHeight(INTERACTION_HEIGHT);
    interaction.setInteractionWidth(INTERACTION_WIDTH);

    display.addPassenger(interaction);

    PersistentDataContainer pdc = interaction.getPersistentDataContainer();
    pdc.set(WORTH, PersistentDataType.INTEGER, amount);

    display.addScoreboardTag(SCOREBOARD_TAG);
    interaction.addScoreboardTag(SCOREBOARD_TAG);
  }

  private static int checkedModel(int model) {
    return switch (model) {
      case MODEL_LARGE, MODEL_MEDIUM, MODEL_SMALL -> model;
      default -> MODEL_SMALL;
    };
  }
}
