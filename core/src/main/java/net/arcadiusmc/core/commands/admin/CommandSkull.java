package net.arcadiusmc.core.commands.admin;

import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CommandSkull extends BaseCommand {

  public CommandSkull() {
    super("skull");
    setDescription("Gets a player's skull");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player>", "Gets a <player>'s skull");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("profile", Arguments.USER)
            .executes(c -> {
              Player player = c.getSource().asPlayer();

              if (player.getInventory().firstEmpty() == -1) {
                throw Exceptions.INVENTORY_FULL;
              }

              User user = Arguments.getUser(c, "profile");

              CompletableFuture.runAsync(() -> {
                ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) item.getItemMeta();

                meta.setOwningPlayer(user.getOfflinePlayer());
                item.setItemMeta(meta);

                Tasks.runSync(() -> player.getInventory().addItem(item));
              });
              return 0;
            })
        );
  }
}