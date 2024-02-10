package net.arcadiusmc.usables.conditions;

import com.mojang.serialization.DataResult;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.BuiltType;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.ObjectType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class TestNeverUsed implements Condition {

  public static final ObjectType<TestNeverUsed> TYPE = BuiltType.<TestNeverUsed>builder()
      .emptyFactory(() -> new TestNeverUsed(false))
      .loader(dynamic -> {
        boolean b = dynamic.asBoolean(false);
        return DataResult.success(new TestNeverUsed(b));
      })
      .saver((value, ops) -> {
        return DataResult.success(ops.createBoolean(value.used));
      })
      .build();

  private boolean used;

  public TestNeverUsed(boolean used) {
    this.used = used;
  }

  @Override
  public boolean test(Interaction interaction) {
    return !used;
  }

  @Override
  public void afterTests(Interaction interaction) {
    used = true;
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Text.format("used={0}", used);
  }
}
