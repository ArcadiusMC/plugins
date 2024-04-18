package net.arcadiusmc.factions.usables;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionMember;
import net.arcadiusmc.factions.Factions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableCodecs;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class AnyFactionMemberTest implements Condition {

  static final ObjectType<AnyFactionMemberTest> TYPE = new AnyFactionTestType();

  @Getter
  private final IntRange reputationRange;

  public AnyFactionMemberTest(IntRange reputationRange) {
    this.reputationRange = reputationRange;
  }

  @Override
  public boolean test(Interaction interaction) {
    Optional<User> userOpt = interaction.getUser();

    if (userOpt.isEmpty()) {
      return false;
    }

    User user = userOpt.get();
    Faction faction = Factions.getManager().getCurrentFaction(user.getUniqueId());

    if (faction == null) {
      return false;
    }

    FactionMember member = faction.getActiveMember(user.getUniqueId());
    if (member == null) {
      return false;
    }

    if (reputationRange == IntRange.UNLIMITED || reputationRange == null) {
      return true;
    }

    return reputationRange.contains(member.getReputation());
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Messages.render("factions.errors.notInAnyFactions")
        .create(interaction.getPlayer().orElse(null));
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}

class AnyFactionTestType implements ObjectType<AnyFactionMemberTest> {

  @Override
  public AnyFactionMemberTest parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    if (!reader.canRead()) {
      return new AnyFactionMemberTest(IntRange.UNLIMITED);
    }

    IntRange range = ArgumentTypes.intRange().parse(reader);
    return new AnyFactionMemberTest(range);
  }

  @Override
  public AnyFactionMemberTest createEmpty() throws UnsupportedOperationException {
    return new AnyFactionMemberTest(IntRange.UNLIMITED);
  }

  @Override
  public <S> DataResult<AnyFactionMemberTest> load(Dynamic<S> dynamic) {
    return UsableCodecs.INT_RANGE.parse(dynamic).map(AnyFactionMemberTest::new);
  }

  @Override
  public <S> DataResult<S> save(@NotNull AnyFactionMemberTest value, @NotNull DynamicOps<S> ops) {
    IntRange range = value.getReputationRange();

    if (range == null || range == IntRange.UNLIMITED) {
      return Results.success(ops.empty());
    }

    return UsableCodecs.INT_RANGE.encodeStart(ops, range);
  }
}
