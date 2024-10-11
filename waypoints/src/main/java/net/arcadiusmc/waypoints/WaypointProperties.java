package net.arcadiusmc.waypoints;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.serialization.Codec.BOOL;
import static com.mojang.serialization.Codec.INT;
import static com.mojang.serialization.Codec.STRING;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.UUID;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.arcadiusmc.waypoints.command.StringListArgument;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.math.vector.Vector3i;

public class WaypointProperties {

  /**
   * Registry of waypoint properties
   */
  public static final Registry<WaypointProperty> REGISTRY = Registries.newFreezable();

  /**
   * Determines if a pole can be destroyed, if set to true, a waypoint will also never be
   * automatically deleted
   */
  public static final WaypointProperty<Boolean> INVULNERABLE
      = new WaypointProperty<>("invulnerable", bool(), BOOL, false)
      .setUpdatesMarker(false);

  /**
   * Determines if the waypoint can be visited by others without invitation, only applies to named
   * regions that wish to still require invitation to visit.
   */
  public static final WaypointProperty<Boolean> PUBLIC
      = new WaypointProperty<>("public", bool(), BOOL, true);

  /**
   * Only applies to named waypoints to determine whether they want to have or disallow the dynmap
   * marker
   */
  public static final WaypointProperty<Boolean> ALLOWS_MARKER
      = new WaypointProperty<>("allows_marker", bool(), BOOL, true);

  /**
   * Only applies to named regions, sets the waypoint's marker icon
   */
  public static final WaypointProperty<String> MARKER_ICON
      = new WaypointProperty<>("marker_icon", greedyString(), STRING, null);

  public static final WaypointProperty<Boolean> REQUIRES_DISCOVERY
      = new WaypointProperty<>("requires_discovery", bool(), BOOL, false)
      .setUpdatesMarker(false);

  public static final WaypointProperty<Integer> DISCOVERY_RANGE
      = new WaypointProperty<>("discovery_range", integer(), INT, null)
      .setUpdatesMarker(false)
      .setCallback((waypoint, oldValue, value) -> {
        WaypointManager manager = waypoint.manager;

        World world = waypoint.getWorld();
        assert world != null;

        manager.discoveryMap.remove(world, waypoint);
        manager.discoveryMap.add(world, waypoint.getDiscoveryBounds(), waypoint);
      });

  public static final WaypointProperty<TextColor> NAME_COLOR
      = new WaypointProperty<>("name_color", Arguments.COLOR, createColorCodec(), null);

  public static final WaypointProperty<Integer> VISITS_DAILY
      = new WaypointProperty<>("visits/daily", integer(), INT, 0)
      .setUpdatesMarker(false);

  public static final WaypointProperty<Integer> VISITS_MONTHLY
      = new WaypointProperty<>("visits/monthly", integer(), INT, 0)
      .setUpdatesMarker(false);

  public static final WaypointProperty<Integer> VISITS_TOTAL
      = new WaypointProperty<>("visits/total", integer(), INT, 0)
      .setUpdatesMarker(false);

  public static final WaypointProperty<ItemStack> DISPLAY_ITEM
      = new WaypointProperty<>("display_material", Arguments.ITEMSTACK, ExtraCodecs.ITEM_CODEC, null)
      .setUpdatesMarker(false);

  public static final WaypointProperty<Float> VISIT_YAW
      = new WaypointProperty<>("visit_rotation/yaw", floatArg(-180, 180), Codec.FLOAT, null)
      .setUpdatesMarker(false);

  public static final WaypointProperty<Float> VISIT_PITCH
      = new WaypointProperty<>("visit_rotation/pitch", floatArg(-90, 90), Codec.FLOAT, null)
      .setUpdatesMarker(false);

  /**
   * Property only used for region poles to determine whether they should display their resident
   * count on the pole.
   */
  public static final WaypointProperty<Boolean> HIDE_RESIDENTS
      = new WaypointProperty<>("hide_residents", bool(), BOOL, false)
      .setUpdatesMarker(false)
      .setCallback((waypoint, oldValue, value) -> {
        waypoint.updateResidentsSign();
      });

  /**
   * The region's name, will be used on the dynmap, if they allow for the marker, and can be used by
   * other players to visit this region with '/visit [name]'
   */
  public static final WaypointProperty<String> NAME
      = new WaypointProperty<>("name", StringArgumentType.string(), Codec.STRING, null)
      .setCallback((waypoint, oldValue, value) -> {
        WaypointManager.getInstance().onRename(waypoint, oldValue, value);
        waypoint.updateNameSign();
      })

      .setValidator((waypoint, newValue) -> {
        DataResult<String> result = Waypoints.validateWaypointName(newValue);

        if (result.error().isEmpty()) {
          return;
        }

        throw Exceptions.format("Invalid waypoint name '{0}': {1}",
            newValue, result.error().get().message()
        );
      });

  public static final WaypointProperty<List<String>> ALIASES
      = new WaypointProperty<>("aliases", new StringListArgument(), Codec.STRING.listOf(), null)
      .setUpdatesMarker(false)

      .setCallback((waypoint, oldValue, value) -> {
        WaypointManager.getInstance().onAliasesUpdate(waypoint, oldValue, value);
      })

      .setValidator((waypoint, newValue) -> {
        for (String s : newValue) {
          NAME.validateValue(waypoint, s);
        }
      });

  /**
   * The UUID of the player that owns the waypoint
   */
  public static final WaypointProperty<UUID> OWNER
      = new WaypointProperty<>("owner", ArgumentTypes.uuid(), ExtraCodecs.INT_ARRAY_UUID, null)
      .setUpdatesMarker(false);

  public static final WaypointProperty<Direction> DIRECTION
      = new WaypointProperty<>("direction", ArgumentTypes.enumType(Direction.class), ExtraCodecs.enumCodec(Direction.class), Direction.EAST)
      .setUpdatesMarker(false)
      .setCallback((waypoint, oldValue, value) -> {
        Vector3i anchor = waypoint.getAnchor();
        if (anchor == null) {
          return;
        }

        // Clear signs before updating
        WorldBounds3i bounds = Bounds3i.of(anchor, 1).toWorldBounds(waypoint.getWorld());
        for (Block block : bounds) {
          BlockState state = block.getState();
          if (!(state instanceof Sign)) {
            continue;
          }
          block.setType(Material.AIR, false);
        }

        waypoint.update(true);
      })
      .setValidator((waypoint, newValue) -> {
        if (newValue.isRotatable()) {
          return;
        }

        throw Exceptions.create("Direction value must not be 'up' or 'down'");
      });

  private static Codec<TextColor> createColorCodec() {
    return INT.xmap(TextColor::color, TextColor::value);
  }
}