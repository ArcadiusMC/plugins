package net.arcadiusmc.usables.conditions;

import static net.arcadiusmc.usables.conditions.InWorldguardRegion.getManager;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class InWorldguardRegion implements Condition {

  static final ObjectType<InWorldguardRegion> TYPE = new InWorldGuardRegionType();

  private final String regionId;

  public InWorldguardRegion(String regionId) {
    this.regionId = regionId;
  }

  @Override
  public boolean test(Interaction interaction) {
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      return false;
    }

    Player player = playerOpt.get();

    Optional<World> worldOpt = interaction.getValue("world", World.class);
    World world = worldOpt.orElseGet(player::getWorld);

    RegionManager manager = getManager(world);
    ProtectedRegion region = manager.getRegion(regionId);

    if (region == null) {
      return false;
    }

    Location l = player.getLocation();

    return region.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Component.text()
        .append(Component.text("Hey!", NamedTextColor.DARK_RED, TextDecoration.BOLD))
        .append(Component.text(" You can't do that here!", NamedTextColor.GRAY))
        .build();
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(regionId);
  }

  static RegionManager getManager(World world) {
    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    RegionManager manager = container.get(BukkitAdapter.adapt(world));
    return manager;
  }
}

class InWorldGuardRegionType implements ObjectType<InWorldguardRegion> {

  @Override
  public InWorldguardRegion parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    int start = reader.getCursor();

    String remaining = reader.getRemaining();
    reader.setCursor(reader.getTotalLength());

    RegionManager manager = getManager(source.getWorld());

    if (manager != null) {
      ProtectedRegion region = manager.getRegion(remaining);

      if (region == null) {
        reader.setCursor(start);
        throw Exceptions.unknown("Worldguard Region", reader, remaining);
      }
    }

    return new InWorldguardRegion(remaining);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    RegionManager manager = getManager(context.getSource().getWorld());

    if (manager == null) {
      return Suggestions.empty();
    }

    return Completions.suggest(builder, manager.getRegions().keySet());
  }

  @Override
  public <S> DataResult<InWorldguardRegion> load(Dynamic<S> dynamic) {
    return dynamic.asString().map(InWorldguardRegion::new);
  }

  @Override
  public <S> DataResult<S> save(@NotNull InWorldguardRegion value, @NotNull DynamicOps<S> ops) {
    return DataResult.success(ops.createString(value.getRegionId()));
  }
}
