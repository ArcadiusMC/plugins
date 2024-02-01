package net.arcadiusmc.core.commands.item;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.PeriodFormat;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.utils.Time;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.RegistryArgument;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

public class ItemCooldownNode extends ItemModifierNode {

  private static final RegistryArgument<Material> MATERIAL_ARG
      = ArgumentTypes.registry(Registry.MATERIAL, "material");

  public ItemCooldownNode() {
    super("item_cooldown", "itemcooldown");
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    var prefixed = factory.withPrefix("<material>");

    prefixed.usage("")
        .addInfo("Shows how long a <material> is on cooldown for you");

    prefixed.usage("<time>")
        .addInfo("Sets your <material>'s cooldown to <time>");
  }

  @Override
  String getArgumentName() {
    return "cooldown";
  }

  @Override
  public void create(LiteralArgumentBuilder<CommandSource> command) {
    command.then(argument("material", MATERIAL_ARG)
        .executes(c -> {
          Player player = c.getSource().asPlayer();
          Material material = c.getArgument("material", Material.class);

          if (!player.hasCooldown(material)) {
            throw ItemMessages.NO_COOLDOWN.get()
                .addValue("material", material)
                .exception(player);
          }

          int remainingTicks = player.getCooldown(material);

          player.sendMessage(
              ItemMessages.COOLDOWN_DISPLAY.get()
                  .addValue("material", material)
                  .addValue("remainingTicks", remainingTicks)
                  .addValue("remaining", PeriodFormat.of(Time.ticksToMillis(remainingTicks)))
                  .create(player)
          );
          return 0;
        })

        .then(argument("cooldown", ArgumentTypes.time())
            .executes(c -> {
              long cooldownTicks = ArgumentTypes.getTicks(c, "cooldown");
              return setCooldown(c, cooldownTicks);
            })
        )

        .then(literal("remove")
            .executes(c -> setCooldown(c, 0))
        )
    );
  }

  private int setCooldown(CommandContext<CommandSource> c, long cooldownTicks)
      throws CommandSyntaxException
  {
    Player player = c.getSource().asPlayer();
    Material material = c.getArgument("material", Material.class);

    player.setCooldown(material, (int) cooldownTicks);

    MessageRender render = cooldownTicks < 1
        ? ItemMessages.REMOVED_COOLDOWN.get()
        : ItemMessages.SET_COOLDOWN.get();

    c.getSource().sendSuccess(
        render
            .addValue("material", material)
            .addValue("remainingTicks", cooldownTicks)
            .addValue("remaining", PeriodFormat.of(Time.ticksToMillis(cooldownTicks)))
            .create(player)
    );
    return 0;
  }
}