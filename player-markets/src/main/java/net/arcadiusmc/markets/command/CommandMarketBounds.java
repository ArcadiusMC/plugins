package net.arcadiusmc.markets.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Collection;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.markets.ClaimHighlighter;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandMarketBounds extends BaseCommand {

  private final MarketsPlugin plugin;

  public CommandMarketBounds(MarketsPlugin plugin) {
    super("market-bounds");
    this.plugin = plugin;

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("hash", IntegerArgumentType.integer())
        .executes(c -> {
          User user = getUserSender(c);
          int hash = c.getArgument("hash", Integer.class);

          Collection<Market> markets = plugin.getManager().getMarkets();
          Market market = null;

          for (Market m : markets) {
            if (m.getRegionName().hashCode() != hash) {
              continue;
            }

            market = m;
            break;
          }

          if (market == null) {
            return 0;
          }

          ClaimHighlighter highlighter = plugin.getHighlighter();
          highlighter.show(user, market);

          return 0;
        })
    );
  }
}
