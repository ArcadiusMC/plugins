package net.arcadiusmc.bank;

import static net.arcadiusmc.Permissions.register;

import com.destroystokyo.paper.ParticleBuilder;
import java.util.stream.Stream;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.bank.ChestGroup.ChestPosition;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.math.Bounds3i;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class VaultDebug implements Runnable {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Permission DEBUG_PERMISSION = register(CommandBankVault.PERMISSION, "debug");

  static boolean drawCoins = false;
  static boolean drawChests = false;
  static boolean drawRoom = false;
  static boolean drawEnterExit = false;

  private final BankPlugin plugin;
  private BukkitTask debugTicking = null;

  public VaultDebug(BankPlugin plugin) {
    this.plugin = plugin;
  }

  @SuppressWarnings("unchecked")
  static Stream<Player> getDebugPlayers() {
    return (Stream<Player>) Bukkit.getOnlinePlayers()
        .stream()
        .filter(player -> player.hasPermission(DEBUG_PERMISSION));
  }

  public void startTicking() {
    stopTicking();
    debugTicking = Tasks.runTimer(this, 1, 1);
  }

  public void stopTicking() {
    debugTicking = Tasks.cancel(debugTicking);
  }

  public boolean isTicking() {
    return Tasks.isScheduled(debugTicking);
  }

  @Override
  public void run() {
    getDebugPlayers().forEach(this::drawDebugFor);
  }

  private void drawDebugFor(Player player) {
    if (drawRoom) {
      BankVault closest = findClosest(player);
      if (closest != null) {
        drawClosest(player, closest);
      }
    }

    BankVault insideOf = getInsideOf(player);
    if (insideOf == null) {
      return;
    }

    if (drawChests) {
      drawChests(player, insideOf);
    }
    if (drawCoins) {
      drawCoins(player, insideOf);
    }
  }

  private static void drawCoins(Player player, BankVault vault) {
    final float len = 1f;
    final float half = len * 0.5f;

    Vector3d up = new Vector3d();
    Vector3d down = new Vector3d();

    for (Vector3f coinPosition : vault.getCoinPositions()) {
      up.set(coinPosition);
      down.set(coinPosition);

      up.y += half;
      down.y -= half;

      line(player, up, down, Color.GREEN);
    }
  }

  private static void drawChests(Player player, BankVault vault) {
    final double lineLength = 1d;
    final double halfLen = lineLength * 2;

    Vector3d chestPos = new Vector3d();

    Vector3d up = new Vector3d();
    Vector3d down = new Vector3d();
    Vector3d forward = new Vector3d();
    Vector3d backward = new Vector3d();

    Vector3d dirMod = new Vector3d();

    final float off = 0.5f;

    for (ChestGroup chestGroup : vault.getChestGroups()) {
      for (ChestPosition position : chestGroup.getPositions()) {
        chestPos.x = position.x() + off;
        chestPos.y = position.y() + off;
        chestPos.z = position.z() + off;

        up.set(chestPos);
        up.y += halfLen;

        down.set(chestPos);
        down.y -= halfLen;

        BlockFace facing = position.facing();
        dirMod.set(facing.getModX(), facing.getModY(), facing.getModZ());
        dirMod.mul(halfLen);

        forward.set(chestPos).add(dirMod);
        backward.set(chestPos).sub(dirMod);

        line(player, up, down, Color.RED);
        line(player, forward, backward, Color.RED);
      }
    }
  }

  private void drawClosest(Player player, BankVault vault) {
    Bounds3i vaultRoom = vault.getVaultRoom();
    Particles.drawBounds(player.getWorld(), vaultRoom, Color.WHITE);
  }

  private BankVault getInsideOf(Player player) {
    for (BankVault value : plugin.getVaultMap().values()) {
      if (!value.getVaultRoom().contains(player)) {
        continue;
      }
      if (!value.getWorldName().equals(player.getWorld().getName())) {
        continue;
      }

      return value;
    }

    return null;
  }

  private BankVault findClosest(Player player) {
    BankVault closest = null;
    double closestDist = Integer.MAX_VALUE;

    Vector3d pPos = new Vector3d(
        player.getX(),
        player.getEyeHeight() + player.getY(),
        player.getZ()
    );
    Vector3d closestPoint = new Vector3d();

    for (BankVault value : plugin.getVaultMap().values()) {
      if (!value.getWorldName().equals(player.getWorld().getName())) {
        continue;
      }

      Bounds3i room = value.getVaultRoom();
      room.joml_getClosestPosition(pPos, closestPoint);
      double dist = chebyshevDistance(closestPoint, pPos);

      if (dist < closestDist) {
        closestDist = dist;
        closest = value;
      }
    }

    return closest;
  }

  private static double chebyshevDistance(Vector3d p1, Vector3d p2) {
    double xDif = Math.abs(p1.x - p2.x);
    double yDir = Math.abs(p1.y - p2.y);
    double zDir = Math.abs(p1.z - p2.z);
    return Math.max(xDif, Math.max(yDir, zDir));
  }

  private static void line(Player player, Vector3d p1, Vector3d p2, Color color) {
    ParticleBuilder builder = Particle.DUST.builder()
        .color(color, 1f)
        .receivers(player);

    final float pointDist = 0.5f;

    Vector3d direction = new Vector3d(p2).sub(p1);
    double len = direction.length();
    direction.normalize();

    Vector3d p = new Vector3d(p1);
    double marchedLen = 0.0d;

    World world = player.getWorld();

    while (marchedLen < len) {
      builder
          .location(world, p.x, p.y, p.z)
          .spawn();

      p.x += (direction.x * pointDist);
      p.y += (direction.y * pointDist);
      p.z += (direction.z * pointDist);

      marchedLen += pointDist;
    }
  }
}
