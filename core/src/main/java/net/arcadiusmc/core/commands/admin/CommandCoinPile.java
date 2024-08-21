package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.core.Coinpile;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.Location;

public class CommandCoinPile extends BaseCommand {

  static final int DEFAULT_AMOUNT = 100;
  static final Model DEFAULT_MODEL = Model.SMALL;

  public CommandCoinPile() {
    super("coinpile");

    setDescription("Spawns a coin pile");
    setPermission(CorePermissions.CMD_COINPILE);
    setAliases("coin-pile", "coinstack", "coin-stack");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> spawn(c.getSource(), DEFAULT_AMOUNT, DEFAULT_MODEL))

        .then(argument("worth", IntegerArgumentType.integer(1))
            .executes(c -> {
              int worth = IntegerArgumentType.getInteger(c, "worth");
              return spawn(c.getSource(), worth, DEFAULT_MODEL);
            })

            .then(argument("model", ArgumentTypes.enumType(Model.class))
                .executes(c -> {
                  int worth = IntegerArgumentType.getInteger(c, "worth");
                  Model model = c.getArgument("model", Model.class);
                  return spawn(c.getSource(), worth, model);
                })
            )
        );
  }

  private int spawn(CommandSource source, int worth, Model model) throws CommandSyntaxException {
    Location location = source.getLocation();
    Coinpile.create(location, worth, model.modelId);

    source.sendSuccess(
        Messages.render("cmd.coinpile.created")
            .addValue("worth", worth)
            .addValue("model", model.name().toLowerCase())
            .create(source)
    );

    return SINGLE_SUCCESS;
  }

  public enum Model {
    LARGE (Coinpile.MODEL_LARGE),
    MEDIUM (Coinpile.MODEL_MEDIUM),
    SMALL (Coinpile.MODEL_SMALL),
    ;

    final int modelId;

    Model(int modelId) {
      this.modelId = modelId;
    }
  }
}
