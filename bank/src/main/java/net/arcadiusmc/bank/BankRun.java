package net.arcadiusmc.bank;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.core.Coinpile;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.Time;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

@Getter
public class BankRun {

  static final Times TIMES = Times.times(
      Duration.ZERO,
      Duration.ofMillis(750),
      Duration.ofMillis(250)
  );

  private final BankPlugin plugin;

  private final Player player;
  private final World world;
  private final BoundingBox boundingBox;

  private BukkitTask tickingTask;
  private int ticks;
  private BossBar bossBar;

  @Setter
  private ItemList startItems = null;

  @Setter
  private int pickedCoins = 0;

  @Setter
  private BankVault vault;

  @Setter
  private String vaultKey;

  @Setter
  private String variant = "default";

  private RunState state = RunState.INACTIVE;

  public BankRun(BankPlugin plugin, Player player, World world) {
    this.plugin = plugin;
    this.player = player;
    this.world = world;
    this.boundingBox = new BoundingBox();
  }

  public void startTicking() {
    stopTicking();
    tickingTask = Tasks.runTimer(this::tick, 1, 1);
  }

  public void stopTicking() {
    tickingTask = Tasks.cancel(tickingTask);
  }

  private long maxTicks() {
    if (state == RunState.ONGOING) {
      return toTicks(vault.getRunTime());
    }

    if (state == RunState.ENDING) {
      return toTicks(vault.getEndingTime());
    }

    return 0l;
  }

  private void updateBossBar() {
    if (bossBar == null) {
      return;
    }

    long maxTicks = maxTicks();

    if (maxTicks < 1) {
      bossBar.setProgress(0);
      return;
    }

    long remainingTicks = maxTicks - ticks;

    double t = ((double) remainingTicks) / maxTicks;
    bossBar.setProgress(t);

    long remainingMillis = Time.ticksToMillis(remainingTicks);
    String remainingDuration = Text.timer(remainingMillis);

    String title = switch (state) {
      case ENDING -> "Run ending! Exit now!! ";
      default -> "Run time left: ";
    };

    bossBar.setTitle(title + remainingDuration);
  }

  private void tick() {
    ticks++;

    if (vault == null) {
      return;
    }

    updateBossBar();

    switch (state) {
      case ONGOING -> {
        long maxTicks = toTicks(vault.getRunTime());

        if (ticks >= maxTicks) {
          beginEndPhase();
        }
      }

      case ENDING -> {
        long maxTicks = toTicks(vault.getEndingTime());
        long remaining = maxTicks - ticks;

        if (remaining < (maxTicks * 0.5) && remaining % 20 == 0 && remaining > 0) {
          long secondsLeft = remaining / 20;

          Component title = Messages.render("bankruns.kickout.title")
              .addValue("secondsLeft", secondsLeft)
              .create(player);

          Component subtitle = Messages.render("bankruns.kickout.subtitle")
              .addValue("secondsLeft", secondsLeft)
              .create(player);

          Title advTitle = Title.title(title, subtitle, TIMES);
          player.showTitle(advTitle);
        }

        if (ticks >= maxTicks) {
          kick(true);
        }
      }
    }
  }

  public void start() {
    bossBar = Bukkit.createBossBar("Bank Run", BarColor.BLUE, BarStyle.SOLID);
    bossBar.setProgress(1d);
    bossBar.setVisible(true);
    bossBar.addPlayer(player);

    startTicking();
    switchState(RunState.ONGOING);
    generateRoom();

    Location entrance = vault.getEnterPosition().toLocation(world);
    player.teleport(entrance);

    startItems = ItemLists.cloneAllItems(player.getInventory());
  }

  public void kick(boolean removeGains) {
    plugin.getSessionMap().remove(player.getUniqueId());

    Location exit = vault.getExitPosition().toLocation(world);
    player.teleport(exit);

    switchState(RunState.INACTIVE);
    clearRoom();
    stopTicking();

    if (bossBar != null) {
      bossBar.setVisible(false);
      bossBar.removeAll();
      bossBar = null;
    }

    if (!removeGains) {
      player.sendMessage(
          Messages.render("bankruns.completed")
              .addValue("coinEarnings", Messages.currency(pickedCoins))
              .create(player)
      );

      return;
    }

    player.sendMessage(Messages.renderText("bankruns.kicked", player));

    User user = Users.get(player);

    if (pickedCoins > 0) {
      user.removeBalance(pickedCoins);
      pickedCoins = 0;
    }

    compareAndRemoveItems();
  }

  private void generateRoom() {
    vault.place(variant, world);
  }

  private void clearRoom() {
    Bounds3i bounds = vault.getVaultRoom();
    if (bounds.equals(Bounds3i.EMPTY)) {
      return;
    }

    WorldBounds3i worldBounds = bounds.toWorldBounds(world);

    for (Entity entity : worldBounds.getEntities()) {
      if (entity instanceof Item) {
        entity.remove();
        continue;
      }

      if (entity.getScoreboardTags().contains(Coinpile.SCOREBOARD_TAG)) {
        entity.remove();
        continue;
      }
    }
  }

  private void compareAndRemoveItems() {
    if (startItems == null) {
      return;
    }

    if (startItems.isEmpty()) {
      player.getInventory().clear();
      return;
    }

    PlayerInventory inventory = player.getInventory();
    ItemList items = ItemLists.fromInventory(inventory);
    ItemList clonedCurrent = ItemLists.cloneAllItems(items);

    Object2IntMap<ItemStack> counted = ItemStacks.countItems(clonedCurrent);

    for (Entry<ItemStack> entry : counted.object2IntEntrySet()) {
      ItemStack item = entry.getKey();

      int totalNew = entry.getIntValue();
      int totalOld = startItems.countMatching(item);

      if (totalNew > totalOld) {
        items.removeMatching(item, totalNew - totalOld);
        continue;
      }

      if (totalNew < totalOld) {
        int required = totalOld - totalNew;

        ItemStack clone = item.clone();
        clone.setAmount(required);

        ItemStacks.giveOrDrop(inventory, clone);
      }
    }
  }

  public void beginEndPhase() {
    long endingTicks = toTicks(vault.getEndingTime());

    if (endingTicks <= 0) {
      kick(false);
      return;
    }

    if (bossBar != null) {
      bossBar.setColor(BarColor.PINK);
    }

    switchState(RunState.ENDING);
  }

  private void switchState(RunState newState) {
    this.state = newState;
    this.ticks = 0;
  }

  private static long toTicks(Duration d) {
    return Time.millisToTicks(d.toMillis());
  }

  public enum RunState {
    INACTIVE,
    ONGOING,
    ENDING,
    ;
  }
}
