package net.arcadiusmc.core.listeners;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import net.arcadiusmc.core.CoreConfig;
import net.arcadiusmc.core.CoreFlags;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.WgUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.spongepowered.math.GenericMath;

public class MobHealthBar implements Listener {

  static final Brightness MAX_BRIGHT = new Brightness(15, 15);

  public static final String HEART = "❤";

  public static final Map<LivingEntity, Component> NAMES = new HashMap<>();
  public static final Map<LivingEntity, BukkitTask> HITMOBS = new HashMap<>();
  public static final Set<TextDisplay> HIT_MARKERS = new ObjectOpenHashSet<>();

  private static final Random RANDOM = new Random();

  public static final double MAX_DAMAGE_INDICATION = 20;

  private final CorePlugin plugin;

  public MobHealthBar(CorePlugin plugin) {
    this.plugin = plugin;
  }

  private static void delay(LivingEntity damaged) {
    BukkitTask task = HITMOBS.get(damaged);
    Tasks.cancel(task);

    task = Tasks.runLater(() -> {
      damaged.setCustomNameVisible(false);
      damaged.customName(NAMES.get(damaged));

      NAMES.remove(damaged);
      HITMOBS.remove(damaged);
    }, 5 * 20);

    HITMOBS.put(damaged, task); //Put delay in map
  }

  public static void showHealthBar(LivingEntity damaged, double finalDamage, boolean autoRemove) {
    String name = damaged.getCustomName();

    // Only affect entities that only show names when player hovers mouse over them:
    // (Note: colored names can get replaced, they return properly anyway)
    if (name != null && !name.contains(HEART)) {

      // Don't change names of entities with always visible names
      // (without hearts in them)
      if (damaged.isCustomNameVisible()) {
        return;
      } else {
        // Save names of player-named entities
        NAMES.put(damaged, damaged.customName());
      }
    }

    // Calculate hearts to show:
    int maxHealth = (int) Math.ceil(damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() / 2);
    int remainingHRTS = (int) Math.ceil((damaged.getHealth() - finalDamage) / 2);

    if (remainingHRTS < 0) {
      remainingHRTS = 0;
    }

    if (remainingHRTS > 20) {
      return;
    }

    // Entities with too many hearts, can at max show 20 hearts, if their
    // health is above that, hearts don't show.
    int heartsToShow = Math.min(maxHealth, 20);

    // Construct name with correct hearts:
    String reds = HEART.repeat(remainingHRTS);
    String grays = HEART.repeat(Math.max(0, heartsToShow - remainingHRTS));

    // Show hearts + set timer to remove hearts
    // By having a Map<LivingEntity, BukkitRunnable>, we can dynamically delay the custom name being
    // turned back into normal
    damaged.setCustomNameVisible(true);
    damaged.customName(
        Component.text()
            .append(Component.text(reds, NamedTextColor.RED))
            .append(Component.text(grays, NamedTextColor.DARK_GRAY))
            .build()
    );

    if (autoRemove) {
      delay(damaged);
    }
  }

  public static void spawnDamageNumber(BoundingBox entityBounds, World world, double damage) {
    double x = RANDOM.nextDouble(entityBounds.getMinX() - 0.5D, entityBounds.getMaxX() + 0.5D);
    double y = RANDOM.nextDouble(entityBounds.getMinY() + 0.5D, entityBounds.getMaxY() + 0.5D);
    double z = RANDOM.nextDouble(entityBounds.getMinZ() - 0.5D, entityBounds.getMaxZ() + 0.5D);

    TextDisplay entity = world.spawn(
        new Location(world, x, y, z),
        TextDisplay.class,

        display -> {
          display.setBackgroundColor(Color.fromARGB(0));
          display.setBillboard(Billboard.CENTER);
          display.setShadowed(false);
          display.setSeeThrough(true);
          display.text(Component.text(String.format("%.2f", damage), damageColor(damage)));
          display.setBrightness(MAX_BRIGHT);
          display.setPersistent(false);

          Transformation trans = display.getTransformation();
          trans.getTranslation().y = -0.125f;
          display.setTransformation(trans);
        }
    );

    HIT_MARKERS.add(entity);

    entity.getScheduler().runAtFixedRate(
        PluginUtil.getPlugin(),
        new Consumer<>() {
          int ticks = 12;

          @Override
          public void accept(ScheduledTask task) {
            if (--ticks < 0) {
              task.cancel();
              entity.remove();
              HIT_MARKERS.remove(entity);

              return;
            }

            entity.teleport(entity.getLocation().add(0, 0.05D, 0));
          }
        },
        entity::remove,
        1, 1
    );
  }

  private static TextColor damageColor(double dmg) {
    double progress = dmg / MAX_DAMAGE_INDICATION;
    progress = GenericMath.clamp(progress, 0.0F, 1.0F);

    return TextColor.lerp((float) progress, NamedTextColor.YELLOW, NamedTextColor.RED);
  }

  public static void shutdown() {
    NAMES.forEach(Entity::customName);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onMobDamage(EntityDamageByEntityEvent event) {
    // Check damager is player or arrow shot by player
    if (!(event.getDamager() instanceof Player)) {
      if (!(event.getDamager() instanceof Projectile proj)) {
        return;
      }

      if (!(proj.getShooter() instanceof Player)) {
        return;
      }
    }

    // Must be alive
    if (!(event.getEntity() instanceof LivingEntity damaged)) {
      return;
    }

    // But not another player, armor stand or Boss mob
    if (event.getEntity() instanceof Player
        || event.getEntity() instanceof ArmorStand
        || event.getEntity() instanceof Boss
    ) {
      return;
    }

    double finalDamage = event.getFinalDamage();
    CoreConfig config = plugin.getCoreConfig();

    Location loc = event.getEntity().getLocation();

    if (config.mobHealthBarsEnabled() && WgUtils.testFlag(loc, CoreFlags.HEALTH_BARS)) {
      showHealthBar(damaged, finalDamage, true);
    }

    if (config.damageNumbersEnabled() && WgUtils.testFlag(loc, CoreFlags.DAMAGE_INDICATORS)) {
      spawnDamageNumber(damaged.getBoundingBox(), damaged.getWorld(), finalDamage);
    }
  }

  //Death messsage
  @EventHandler(ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!event.getDeathMessage().contains("❤")) {
      return;
    }

    var lastDamage = event.getEntity().getLastDamageCause();
    if (!(lastDamage instanceof EntityDamageByEntityEvent damageEvent)) {
      return;
    }

    String name = Text.prettyEnumName(damageEvent.getDamager().getType());
    String message = event.getDeathMessage().replaceAll("❤", "") + name;

    event.setDeathMessage(message);
  }
}