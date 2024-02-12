package net.arcadiusmc.sellshop.listeners;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.sellshop.ItemSeller;
import net.arcadiusmc.sellshop.SellMessages;
import net.arcadiusmc.sellshop.SellResult;
import net.arcadiusmc.sellshop.SellShopPlugin;
import net.arcadiusmc.sellshop.UserShopData;
import net.arcadiusmc.sellshop.data.ItemSellData;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Tasks;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.scheduler.BukkitTask;

class AutoSellListener implements Listener {

  // Session map to update messages with correct cumulative amounts instead of displaying a
  // static 'You sold X for Y' message that doesn't change
  private static final Map<UUID, AutoSellSession> SESSIONS = new Object2ObjectOpenHashMap<>();
  public static final int SESSION_TIMEOUT = 20 * 5; // 5 seconds

  private final SellShopPlugin plugin;

  public AutoSellListener(SellShopPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    var user = Users.get(player);
    var earnings = user.getComponent(UserShopData.class);
    var item = event.getItem();
    var stack = item.getItemStack();
    var mat = stack.getType();

    if (!earnings.getAutoSelling().contains(stack.getType())
        // Don't sell named items or items with lore if the
        // user doesn't want to
        || !ItemSeller.matchesFilters(user, mat, stack)
    ) {
      return;
    }

    ItemSellData price = this.plugin.getDataSource().getGlobalPrices().getData(mat);

    if (price == null) {
      return;
    }

    SellResult result = ItemSeller.itemPickup(user, stack, price)
        .run(false);

    if (result.getSold() <= 0) {
      return;
    }

    AutoSellSession session = SESSIONS.get(player.getUniqueId());

    // If existing player session has different type of item
    // then cancel that session
    if (session != null && session.material != mat) {
      session.expire();
      Tasks.cancel(session.task);
      session = null;
    }

    if (session == null) {
      session = new AutoSellSession(mat, user);
      SESSIONS.put(player.getUniqueId(), session);
    }

    // Increment session amount
    session.amount += result.getSold();
    session.earned += result.getEarned();

    // Delay timeout task
    Tasks.cancel(session.task);

    AutoSellSession finalSession = session;
    session.task = Tasks.runLater(() -> {
      SESSIONS.remove(player.getUniqueId());
      finalSession.expire();
    }, SESSION_TIMEOUT);

    player.sendActionBar(SellMessages.soldItems(player, session.amount, session.earned, mat));

    // Play sound
    player.playSound(
        Sound.sound(
            org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            Sound.Source.MASTER,
            2F, 1F
        )
    );

    if (stack.getAmount() <= 0) {
      event.setCancelled(true);
      item.remove();
    } else {
      event.getItem().setItemStack(stack);
    }
  }

  @RequiredArgsConstructor
  static class AutoSellSession {

    private final Material material;
    private final User user;

    private int amount;
    private int earned;

    private BukkitTask task;

    public void expire() {
      ItemSeller.log(user, material, amount, earned);
      user.sendMessage(SellMessages.soldItemsTotal(user, amount, earned, material));
    }
  }
}
