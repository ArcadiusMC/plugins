package net.arcadiusmc.bank;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.delphi.Delphi;
import net.arcadiusmc.delphi.DelphiProvider;
import net.arcadiusmc.delphi.DocumentView;
import net.arcadiusmc.delphi.resource.ResourcePath;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.Time;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.forthecrown.grenadier.Grenadier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
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
import org.joml.Vector3i;
import org.slf4j.Logger;

@Getter
public class BankRun {

  private static final Logger LOGGER = Loggers.getLogger();

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

  private char[] innerVaultCode;

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
    spawnPage();
    setInnerVault(false);
    generateInnerVault();

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
    killPage();
    setInnerVault(false);
    killInnerVault();

    if (bossBar != null) {
      bossBar.setVisible(false);
      bossBar.removeAll();
      bossBar = null;
    }

    if (!removeGains) {
      NamespacedKey advancementKey = vault.getAdvancementKey();
      if (advancementKey != null) {
        giveAdvancement(advancementKey);
      }

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

  private void giveAdvancement(NamespacedKey key) {
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) {
      LOGGER.warn("Unknown advancement '{}' cannot give", key);
      return;
    }

    Collection<String> criteria = adv.getCriteria();
    AdvancementProgress progress = player.getAdvancementProgress(adv);

    if (progress.isDone()) {
      return;
    }

    for (String criterion : criteria) {
      progress.awardCriteria(criterion);
    }
  }

  private void spawnPage() {
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    FullPosition position = vault.getMenuExitPosition();
    if (position.isNullLocation()) {
      return;
    }

    Delphi delphi = DelphiProvider.get();

    delphi
        .openDocument(getPagePath(), player)
        .ifError(e -> {
          LOGGER.error("Error opening page for {}: {}", player.getName(), e.getMessage());
        })
        .ifSuccess(view -> {
          view.moveTo(position.toLocation(world));
        });
  }

  private String getPagePath() {
    return "bank-page:exit.xml?vault=" + vaultKey;
  }

  public void killPage() {
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    Delphi delphi = DelphiProvider.get();
    List<DocumentView> openViews = delphi.getOpenViews(player);

    for (DocumentView openView : openViews) {
      ResourcePath path = openView.getPath();
      if (!path.toString().equals(getPagePath())) {
        continue;
      }

      openView.close();
    }
  }

  void killInnerVault() {
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    Delphi delphi = DelphiProvider.get();

    delphi.getOpenViews(player).stream()
        .filter(view -> view.getPath().toString().startsWith("bank-page:numpad.xml?vault="))
        .forEach(DocumentView::close);
  }

  void generateInnerVault() {
    InnerVault inner = vault.getInnerVault();
    if (inner == null) {
      return;
    }

    Random random = new Random();

    // Generate code
    int codeLength = inner.getCodeLength();
    innerVaultCode = new char[codeLength];
    generateRandomCode(innerVaultCode, random);

    // Place hints
    char[] hintBuf = new char[codeLength];
    List<FacingPosition> positions = inner.getCodePositions();

    Collections.shuffle(positions, random);

    for (int i = 0; i < positions.size(); i++) {
      FacingPosition pos = positions.get(i);

      Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
      WallSign data = (WallSign) Material.OAK_WALL_SIGN.createBlockData();

      data.setFacing(pos.facing());
      block.setBlockData(data, false);

      Sign state = (Sign) block.getState();
      SignSide side = state.getSide(Side.FRONT);

      String str;
      DyeColor color;

      if (i == 0) {
        str = String.valueOf(innerVaultCode);
        color = DyeColor.LIGHT_GRAY;
      } else {
        generateRandomCode(hintBuf, random);
        str = String.valueOf(hintBuf);
        color = DyeColor.GRAY;
      }

      side.setColor(color);
      side.setGlowingText(true);
      side.line(1, Component.text("Vault code:"));
      side.line(2, Component.text(str));

      state.update(true, false);
    }

    // Spawn numpad
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    FullPosition numpadPos = inner.getNumpadPosition();
    if (numpadPos.isNullLocation()) {
      return;
    }

    Delphi delphi = DelphiProvider.get();

    delphi.openDocument("bank-page:numpad.xml?vault=" + vaultKey, player)
        .ifError(e -> {
          LOGGER.error("Failed to open numpad page for inner vault", e);
        })
        .ifSuccess(view -> {
          Location l = numpadPos.toLocation(world);
          view.moveTo(l);
        });
  }

  void generateRandomCode(char[] out, Random random) {
    final char[] potentials = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    for (int i = 0; i < out.length; i++) {
      char ch = potentials[random.nextInt(potentials.length)];
      out[i] = ch;
    }
  }

  public void setInnerVault(boolean open) {
    InnerVault inner = vault.getInnerVault();
    if (inner == null) {
      return;
    }

    String structureName = open
        ? inner.getOpenVaultStructure()
        : inner.getClosedVaultStructure();

    if (Strings.isNullOrEmpty(structureName)) {
      return;
    }

    Vector3i pos = inner.getPosition();
    if (pos == null) {
      return;
    }

    Commands.execute(
        Grenadier.consoleSource().silent(),

        "execute in %s run struct place %s position=%s %s %s",
        world.getKey(),
        structureName,
        pos.x, pos.y, pos.z
    );
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
    ArcadiusServer server = ArcadiusServer.server();

    for (Entity entity : worldBounds.getEntities()) {
      if (entity instanceof Item) {
        entity.remove();
        continue;
      }

      if (!server.isCoinPile(entity)) {
        continue;
      }

      entity.remove();
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
