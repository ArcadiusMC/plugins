package net.arcadiusmc.items.guns;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.items.CallbackComponent;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.ListTag;
import net.forthecrown.nbt.TagTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

@Getter @Setter
public abstract class Gun extends ItemComponent implements CallbackComponent {

  public static final Sound GUN_PICKUP_SOUND = Sound.sound()
      .type(Key.key("arcadiusmc:guns.pickup"))
      .build();

  static final Random GUN_RANDOM = new Random();

  static final double SOUND_RADIUS = 50;

  static final int BETWEEN_RELOADS = 5;

  static final String RELOAD_CHAR = "\u258E";
  static final int CHAR_COUNT = 50;

  public int remainingAmmo;
  public int remainingClipAmmo;

  private int clipSize;
  private int maxAmmo;
  private int ammoUse;
  private int firingDelay;
  private int reloadDelay;
  private int reloadAmount;

  private float baseDamage;
  private float screenKickYaw;
  private float screenKickPitch;

  private int delayTicks;
  private int reloadTicks;
  private int reloadInTicks;

  private int shootInTicks;

  private final List<Sound> sounds = new ArrayList<>();

  private CompoundTag upgradeLevels;


  /**
   * Adds ammo to this gun
   * @param ammo Ammo to add
   * @return The amount of ammo that could not be added
   */
  public int addAmmo(int ammo) {
    if (maxAmmo < 1) {
      remainingAmmo += ammo;
      return 0;
    }

    int untilMax = maxAmmo - remainingAmmo;

    if (untilMax < 1) {
      return ammo;
    }

    if (untilMax >= ammo) {
      remainingAmmo += ammo;
      return 0;
    }

    int add = ammo - untilMax;
    int failed = untilMax - add;

    remainingAmmo += add;

    return failed;
  }

  public void tick(Player player) {
    if (delayTicks > 0) {
      delayTicks--;
    }

    if (reloadTicks > 0) {
      reloadTicks--;

      if (reloadTicks <= 0) {
        reloadInTicks = BETWEEN_RELOADS;
      }
    }

    if (reloadInTicks > 0) {
      reloadInTicks--;

      if (reloadInTicks <= 0 && reloadAmount >= 1) {
        reload();
      }
    }

    if (shootInTicks > 0) {
      shootInTicks--;

      if (shootInTicks < 1) {
        use(player);
      }
    }
  }

  public void reload() {
    reload(reloadAmount);
  }

  public void reload(int reloadAmount) {
    int requiredBullets = clipSize - remainingClipAmmo;

    if (requiredBullets < 1) {
      return;
    }

    int add = Math.min(remainingAmmo, requiredBullets);

    if (reloadAmount > 0) {
      add = Math.min(add, reloadAmount);
    }

    if (add < 1) {
      return;
    }

    remainingAmmo -= add;
    remainingClipAmmo += add;

    reloadTicks = reloadDelay;
  }

  public void reloadInstantly() {
    reload(-1);
    reloadTicks = 0;
    reloadInTicks = 0;
  }


  public final void use(Player player) {
    if (!canFire()) {
      return;
    }

    boolean success = onUse(player, GUN_RANDOM);

    if (!success) {
      return;
    }

    if (!hasUnlimitedAmmo()) {
      remainingClipAmmo -= ammoUse;

      if (remainingClipAmmo < 1) {
        reload();
      }
    }

    reloadInTicks = 0;

    Location loc = player.getLocation();

    float screenKickYaw = Math.abs(this.screenKickYaw);
    float screenKickPitch = Math.abs(this.screenKickPitch);

    float yaw;
    float pitch;

    if (screenKickYaw > 0) {
      yaw = loc.getYaw() + GUN_RANDOM.nextFloat(-screenKickYaw, screenKickYaw);
    } else {
      yaw = loc.getYaw();
    }

    if (screenKickPitch > 0) {
      pitch = loc.getPitch() + GUN_RANDOM.nextFloat(-screenKickPitch, 0);
    } else {
      pitch = loc.getPitch();
    }

    player.setRotation(yaw, pitch);

    World world = player.getWorld();

    final double x = loc.x();
    final double y = loc.y();
    final double z = loc.z();

    for (Sound sound : sounds) {
      world.getNearbyPlayers(loc, SOUND_RADIUS).forEach(player1 -> {
        player1.playSound(sound, x, y, z);
      });
    }

    delayTicks = firingDelay;
  }

  protected abstract boolean onUse(Player player, Random random);

  public boolean canFire() {
    if (reloadTicks > 0 || delayTicks > 0) {
      return false;
    }

    if (hasUnlimitedAmmo()) {
      return true;
    }

    if (ammoUse <= remainingClipAmmo) {
      return true;
    }

    if (remainingAmmo < 1) {
      return false;
    }

    reload();
    return false;
  }

  public Component hotbarText() {
    if (reloadTicks > 0) {
      return reloadText();
    }

    return ammoText();
  }

  public Component reloadText() {
    float progress = ((float) reloadTicks) / reloadDelay;
    int red = (int) (CHAR_COUNT * progress);
    int green = CHAR_COUNT - red;

    String redText = RELOAD_CHAR.repeat(red);
    String greenText = RELOAD_CHAR.repeat(green);

    return textOfChildren(text(redText, NamedTextColor.RED), text(greenText, NamedTextColor.GREEN))
        .decorate(TextDecoration.BOLD);
  }

  public Component ammoText() {
    if (hasUnlimitedAmmo()) {
      return text("Unlimited Ammo");
    }

    Component prefix = Text.formatNumber(remainingClipAmmo);

    if (maxAmmo == -1) {
      return prefix;
    }

    return textOfChildren(prefix, text("/"), Text.formatNumber(remainingAmmo));
  }

  public void writeLore(TextWriter writer) {
    writer.field("Base damage", Text.formatNumber(baseDamage));

    Component ammo;

    if (maxAmmo == -1) {
      ammo = Text.formatNumber(remainingAmmo + remainingClipAmmo);
    } else {
      ammo = text()
          .append(Text.formatNumber(remainingAmmo + remainingClipAmmo))
          .append(text("/"))
          .append(Text.formatNumber(maxAmmo))
          .build();
    }

    writer.field("Ammo", ammo);
    writer.field("Magazine size", clipSize);
  }

  public boolean hasUnlimitedAmmo() {
    return remainingAmmo == -1 || remainingClipAmmo == -1 || ammoUse < 1;
  }

  @Override
  public void onInteractBlock(PlayerInteractEvent event, EquipmentSlot slot) {
    onShootEvent(event.getPlayer(), slot);
    event.setCancelled(true);
  }

  @Override
  public void onInteractEntity(PlayerInteractEntityEvent event, EquipmentSlot slot) {
    onShootEvent(event.getPlayer(), slot);
    event.setCancelled(true);
  }

  private void onShootEvent(Player player, EquipmentSlot slot) {
    if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND) {
      return;
    }

    if (slot == EquipmentSlot.OFF_HAND) {
      shootInTicks = 3;
    } else {
      use(player);
    }
  }

  @Override
  public void onPickup(EntityPickupItemEvent event) {
    event.getEntity().playSound(GUN_PICKUP_SOUND);

    event.getEntity().sendMessage(
        Component.text("Picked up ")
            .color(NamedTextColor.GOLD)
            .append(Text.itemDisplayName(item.getHandle()))
    );
  }

  @Override
  public void save(CompoundTag tag) {
    tag.putInt("remaining_ammo", remainingAmmo);
    tag.putInt("remaining_clip_ammo", remainingClipAmmo);

    tag.putInt("reload_ticks", reloadTicks);
    tag.putInt("delay_ticks", delayTicks);
    tag.putInt("reload_in_ticks", reloadInTicks);

    tag.putInt("max_ammo", maxAmmo);
    tag.putInt("ammo_use", ammoUse);
    tag.putInt("clip_size", clipSize);

    tag.putInt("firing_delay", firingDelay);
    tag.putInt("reload_delay", reloadDelay);
    tag.putInt("reload_amount", reloadAmount);

    tag.putInt("shoot_in_ticks", shootInTicks);

    tag.putFloat("base_damage", baseDamage);
    tag.putFloat("screen_kick_pitch", screenKickPitch);
    tag.putFloat("screen_kick_yaw", screenKickYaw);

    if (!sounds.isEmpty()) {
      ListTag list = BinaryTags.listTag();
      for (Sound sound : sounds) {
        CompoundTag soundTag = BinaryTags.compoundTag();
        soundTag.putString("id", sound.name().asString());
        soundTag.putFloat("pitch", sound.pitch());
        soundTag.putFloat("volume", sound.volume());

        list.add(soundTag);
      }

      tag.put("sounds", list);
    }

    if (upgradeLevels != null) {
      tag.put("upgrade_levels", upgradeLevels);
    }
  }

  @Override
  public void load(CompoundTag tag) {
    remainingAmmo = tag.getInt("remaining_ammo", -1);
    remainingClipAmmo = tag.getInt("remaining_clip_ammo", -1);

    reloadTicks = tag.getInt("reload_ticks", 0);
    delayTicks = tag.getInt("delay_ticks", 0);
    reloadInTicks = tag.getInt("reload_in_ticks", 0);

    maxAmmo = tag.getInt("max_ammo", -1);
    ammoUse = tag.getInt("ammo_use", 1);
    clipSize = tag.getInt("clip_size", 10);

    firingDelay = tag.getInt("firing_delay", 10);
    reloadDelay = tag.getInt("reload_delay", 40);
    reloadAmount = tag.getInt("reload_amount", -1);

    shootInTicks = tag.getInt("shoot_in_ticks", 0);

    baseDamage = tag.getFloat("base_damage", 1f);
    screenKickYaw = tag.getFloat("screen_kick_yaw", 1.25f);
    screenKickPitch = tag.getFloat("screen_kick_pitch", 1.25f);

    sounds.clear();
    if (tag.contains("sounds", TagTypes.listType())) {
      ListTag list = tag.getList("sounds", TagTypes.compoundType());

      for (int i = 0; i < list.size(); i++) {
        CompoundTag soundTag = list.get(i, TagTypes.compoundType());
        Sound loaded = loadSound(soundTag);

        if (loaded == null) {
          continue;
        }

        sounds.add(loaded);
      }
    }

    upgradeLevels = tag.getCompound("upgrade_levels");
  }

  private Sound loadSound(CompoundTag tag) {
    String id = tag.getString("id");

    if (Strings.isNullOrEmpty(id)) {
      return null;
    }

    Key key = NamespacedKey.fromString(id);

    if (key == null) {
      return null;
    }

    Sound.Builder builder = Sound.sound().type(key);

    builder.pitch(tag.getFloat("pitch", 1f));
    builder.volume(tag.getFloat("volume", 1f));

    return builder.build();
  }
}
