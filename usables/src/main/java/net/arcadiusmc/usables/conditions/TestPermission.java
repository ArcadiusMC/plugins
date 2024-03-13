package net.arcadiusmc.usables.conditions;

import com.mojang.serialization.DataResult;
import net.arcadiusmc.usables.BuiltType;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.forthecrown.grenadier.Completions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.Nullable;

public class TestPermission implements Condition {

  static final ObjectType<TestPermission> TYPE = BuiltType.<TestPermission>builder()
      .saver((value, ops) -> DataResult.success(ops.createString(value.permission)))
      .loader(dynamic -> dynamic.asString().map(TestPermission::new))
      .parser((reader, source) -> {
        String remaining = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return new TestPermission(remaining);
      })
      .suggester((context, builder) -> {
        var pl = Bukkit.getPluginManager();
        return Completions.suggest(builder, pl.getPermissions().stream().map(Permission::getName));
      })
      .build();

  private final String permission;

  public TestPermission(String permission) {
    this.permission = permission;
  }

  @Override
  public boolean test(Interaction interaction) {
    return interaction.getPlayer()
        .map(player -> player.hasPermission(permission))
        .orElse(false);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Component.text("You do not have permission to do this", NamedTextColor.GRAY);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(permission);
  }
}
