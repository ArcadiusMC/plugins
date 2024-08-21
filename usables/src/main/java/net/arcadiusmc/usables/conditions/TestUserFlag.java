package net.arcadiusmc.usables.conditions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import lombok.Getter;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.user.UserFlags;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import org.jetbrains.annotations.NotNull;

public class TestUserFlag implements Condition {

  static final TestUserFlagType TYPE = new TestUserFlagType();

  @Getter
  private final String flag;

  public TestUserFlag(String flag) {
    this.flag = flag;
  }

  @Override
  public boolean test(Interaction interaction) {
    return interaction.getPlayer()
        .map(player -> {
          UserFlags flags = Users.getService().getFlags();
          return flags.hasFlag(player.getUniqueId(), flag);
        })
        .orElse(false);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}

class TestUserFlagType implements ObjectType<TestUserFlag> {

  @Override
  public TestUserFlag parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    String flag = StringArgumentType.greedyString().parse(reader);
    return new TestUserFlag(flag);
  }

  @Override
  public <S> DataResult<TestUserFlag> load(Dynamic<S> dynamic) {
    return dynamic.asString().map(TestUserFlag::new);
  }

  @Override
  public <S> DataResult<S> save(@NotNull TestUserFlag value, @NotNull DynamicOps<S> ops) {
    return DataResult.success(ops.createString(value.getFlag()));
  }
}