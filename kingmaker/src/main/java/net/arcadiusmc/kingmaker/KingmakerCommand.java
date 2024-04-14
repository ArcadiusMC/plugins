package net.arcadiusmc.kingmaker;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.EntityRef;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.spongepowered.math.vector.Vector2i;

@CommandFile("kingmaker.gcn")
public class KingmakerCommand {

  private final Kingmaker kingmaker;
  private final KingmakerPlugin plugin;

  public KingmakerCommand(KingmakerPlugin plugin) {
    this.plugin = plugin;
    this.kingmaker = plugin.getKingmaker();
  }

  void reloadConfig(CommandSource source) {
    plugin.reloadConfig();
    source.sendSuccess(Messages.renderText("kingmaker.reloaded.config"));
  }

  void reloadPlugin(CommandSource source) {
    plugin.load();
    source.sendSuccess(Messages.renderText("kingmaker.reloaded.plugin"));
  }

  void savePlugin(CommandSource source) {
    plugin.save();
    source.sendSuccess(Messages.renderText("kingmaker.saved"));
  }

  void getMonarch(CommandSource source) throws CommandSyntaxException {
    UUID monarchId = kingmaker.getMonarchId();

    if (monarchId == null) {
      throw Messages.render("kingmaker.errors.noMonarch")
          .exception(source);
    }

    User user = Users.get(monarchId);

    source.sendMessage(
        Messages.render("kingmaker.get")
            .addValue("player", user)
            .create(source)
    );
  }

  void setMonarch(CommandSource source, @Argument("player") User user) {
    kingmaker.setMonarch(user);

    source.sendSuccess(
        Messages.render("kingmaker.set")
            .addValue("player", user)
            .create(source)
    );
  }

  void unsetMonarch(CommandSource source) {
    kingmaker.setMonarch(null);
    source.sendSuccess(Messages.render("kingmaker.unset").create(source));
  }

  void listStands(CommandSource source) {
    List<MonarchStand> stands = kingmaker.getStands();
    TextJoiner joiner = TextJoiner.newJoiner();

    ListIterator<MonarchStand> it = stands.listIterator();
    while (it.hasNext()) {
      MonarchStand n = it.next();
      int index = it.nextIndex();

      Component display;
      EntityRef ref = n.ref();
      Entity entity = ref.get();

      if (entity != null) {
        display = Component.text()
            .append(
                Component.text("["),
                entity.teamDisplayName().hoverEvent(null),
                Component.text("]")
            )

            .clickEvent(Text.locationClickEvent(entity.getLocation()))
            .hoverEvent(Component.text("Click to teleport!"))
            .build();
      } else {
        World world = ref.getWorld();
        Vector2i chunk = ref.getChunk().mul(Vectors.CHUNK_SIZE);
        Location l = new Location(world, chunk.x(), 100, chunk.y());

        display = Component.text("[Unable to find entity]")
            .hoverEvent(Component.text("Click to teleport!"))
            .clickEvent(Text.locationClickEvent(l));
      }

      joiner.add(
          Messages.render("kingmaker.stands.list.format")
              .addValue("index", index)
              .addValue("entity", display)
              .create(source)
      );
    }

    source.sendMessage(
        Messages.render("kingmaker.stands.list.header")
            .addValue("list", joiner.asComponent())
            .create(source)
    );
  }

  void clearStands(CommandSource source) {
    kingmaker.clearStands();
    source.sendSuccess(Messages.renderText("kingmaker.stands.cleared"));
  }

  void removeStand(CommandSource source, @Argument("index") int index)
      throws CommandSyntaxException
  {
    int size = kingmaker.getStands().size();
    Commands.ensureIndexValid(index, size);

    kingmaker.removeStand(index - 1);

    source.sendSuccess(
        Messages.render("kingmaker.stands.removed")
            .addValue("index", index)
            .create(source)
    );
  }

  void addStand(CommandSource source, @Argument("entity") Entity entity)
      throws CommandSyntaxException
  {
    for (MonarchStand stand : kingmaker.getStands()) {
      if (!stand.ref().getUniqueId().equals(entity.getUniqueId())) {
        continue;
      }

      throw Messages.render("kingmaker.errors.standAdded")
          .addValue("entity", entity.teamDisplayName())
          .exception(source);
    }

    MonarchStand stand = new MonarchStand(EntityRef.of(entity), true, true);
    kingmaker.addStand(stand);

    source.sendSuccess(Messages.renderText("kingmaker.stands.added"));
  }

  void updateAll(CommandSource source) {
    kingmaker.updateStands();
    source.sendSuccess(Messages.renderText("kingmaker.stands.updatedAll", source));
  }
}
