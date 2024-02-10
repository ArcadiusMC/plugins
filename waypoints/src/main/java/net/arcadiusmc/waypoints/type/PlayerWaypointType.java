package net.arcadiusmc.waypoints.type;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import java.util.Optional;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserLookup.LookupEntry;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.DisplayIntent;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WMessages;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointHomes;
import net.arcadiusmc.waypoints.WaypointManager;
import net.arcadiusmc.waypoints.WaypointProperties;
import net.arcadiusmc.waypoints.Waypoints;
import org.apache.commons.lang3.mutable.Mutable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public class PlayerWaypointType extends WaypointType {

  public static final Material[] PLAYER_COLUMN = {
      Material.STONE_BRICKS,
      Material.STONE_BRICKS,
      Material.CHISELED_STONE_BRICKS,
  };

  public PlayerWaypointType() {
    super("Player-Made", PLAYER_COLUMN);
  }

  public PlayerWaypointType(String displayName, Material[] column) {
    super(displayName, column);
  }

  @Override
  public void onCreate(User creator, Vector3i topPos, boolean copy) throws CommandSyntaxException {
    if (copy) {
      return;
    }

    Waypoints.validateMoveInCooldown(creator);

    var alreadyOwned = WaypointHomes.getHome(creator);
    if (alreadyOwned.isEmpty()) {
      return;
    }

    throw WExceptions.homeAlreadySet(alreadyOwned.get());
  }

  @Override
  public void onPostCreate(Waypoint waypoint, User creator) {
    if (waypoint.get(WaypointProperties.OWNER) == null) {
      waypoint.set(WaypointProperties.OWNER, creator.getUniqueId());
    }

    waypoint.addResident(creator.getUniqueId());

    creator.setTimeToNow(TimeField.LAST_MOVEIN);
    creator.sendMessage(WMessages.HOME_WAYPOINT_SET);
  }

  @Override
  public @NotNull Bounds3i createBounds() {
    var config = WaypointManager.getInstance().config();
    return boundsFromSize(config.playerWaypointSize);
  }

  @Override
  public Optional<CommandSyntaxException> isValid(Waypoint waypoint) {
    return Waypoints.isValidWaypointArea(
        waypoint.getPosition(),
        this,
        waypoint.getWorld(),
        false
    );
  }

  @Override
  public Vector3d getVisitPosition(Waypoint waypoint) {
    return waypoint.getPosition()
        .toDouble()
        .add(0.5, getColumn().length, 0.5);
  }

  @Override
  public boolean isDestroyed(Waypoint waypoint) {
    return WaypointTypes.isDestroyed(getColumn(), waypoint.getPosition(), waypoint.getWorld());
  }

  @Override
  protected boolean internalIsBuildable() {
    return true;
  }

  @Override
  public void writeHover(TextWriter writer, Waypoint waypoint, Mutable<Boolean> written) {
    getOwner(waypoint).ifPresent(user -> {
      written.setValue(true);
      writer.field("Owner", user.displayName(writer.viewer(), DisplayIntent.HOVER_TEXT));
    });
  }

  @Override
  public boolean canEdit(User user, Waypoint waypoint) {
    return getOwner(waypoint).map(user1 -> Objects.equals(user1, user)).orElse(false);
  }

  @Override
  public ItemStack getDisplayItem(Waypoint waypoint) {
    return getOwner(waypoint)
        .map(User::getProfile)
        .map(playerProfile -> {
          return ItemStacks.headBuilder()
              .setProfile(playerProfile)
              .build();
        })
        .orElse(null);
  }

  Optional<User> getOwner(Waypoint waypoint) {
    var ownerId = waypoint.get(WaypointProperties.OWNER);

    if (ownerId == null || Objects.equals(ownerId, Waypoints.NIL_UUID)) {
      return Optional.empty();
    }

    UserService service = Users.getService();
    if (!service.userLoadingAllowed()) {
      return Optional.empty();
    }

    LookupEntry entry = service.getLookup().getEntry(ownerId);
    if (entry == null) {
      return Optional.empty();
    }

    return Optional.of(service.getUser(entry));
  }
}