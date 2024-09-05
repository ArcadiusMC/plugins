package net.arcadiusmc.vault;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.currency.Currency;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.spongepowered.math.GenericMath;

public class EconomyInterface extends AbstractEconomy {

  void registerService(Plugin plugin) {
    Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.Highest);
  }

  private final String name;
  private final Currency currency;

  public EconomyInterface(String name, Currency currency) {
    this.name = name;
    this.currency = currency;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return "Arcadius-" + name;
  }

  @Override
  public boolean hasBankSupport() {
    return false;
  }

  @Override
  public int fractionalDigits() {
    return 0;
  }

  @Override
  public String format(double amount) {
    return Text.NUMBER_FORMAT.format(amount);
  }

  @Override
  public String currencyNamePlural() {
    return currency.pluralName();
  }

  @Override
  public String currencyNameSingular() {
    return currency.singularName();
  }

  private Optional<UUID> getPlayerId(String playerName) {
    var service = Users.getService();
    var entry = service.getLookup().query(playerName);

    if (entry == null) {
      return Optional.empty();
    }

    return Optional.of(entry.getUniqueId());
  }

  @Override
  public boolean hasAccount(String playerName) {
    return true;
  }

  @Override
  public boolean hasAccount(String playerName, String worldName) {
    return true;
  }

  @Override
  public double getBalance(String playerName) {
    return getPlayerId(playerName).map(currency::get).orElse(0);
  }

  @Override
  public double getBalance(String playerName, String world) {
    return getPlayerId(playerName).map(currency::get).orElse(0);
  }

  @Override
  public boolean has(String playerName, double amount) {
    return getPlayerId(playerName)
        .map(playerId -> currency.get(playerId) >= amount)
        .orElse(false);
  }

  @Override
  public boolean has(String playerName, String worldName, double amount) {
    return getPlayerId(playerName)
        .map(playerId -> currency.get(playerId) >= amount)
        .orElse(false);
  }

  @Override
  public EconomyResponse withdrawPlayer(String playerName, double amount) {
    return getPlayerId(playerName)
        .map(playerId -> {
          int reduction = GenericMath.floor(amount);
          int currentBalance = currency.get(playerId);

          if (currentBalance < amount) {
            return new EconomyResponse(
                amount, currentBalance, ResponseType.FAILURE,
                "Cannot afford " + format(amount) + " " + currencyNamePlural()
            );
          }

          int nBal = currentBalance - reduction;
          currency.set(playerId, nBal);

          return new EconomyResponse(amount, nBal, ResponseType.SUCCESS, "");
        })
        .orElseGet(() -> {
          return new EconomyResponse(amount, 0, ResponseType.FAILURE, "Failed to find playerId");
        });
  }

  @Override
  public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
    return withdrawPlayer(playerName, amount);
  }

  @Override
  public EconomyResponse depositPlayer(String playerName, double amount) {
    return getPlayerId(playerName)
        .map(playerId -> {
          int addition = GenericMath.floor(amount);
          int nBal = currency.get(playerId) + addition;

          currency.set(playerId, nBal);

          return new EconomyResponse(amount, nBal, ResponseType.SUCCESS, "");
        })
        .orElseGet(() -> {
          return new EconomyResponse(amount, 0, ResponseType.FAILURE, "Failed to find playerId");
        });
  }

  @Override
  public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
    return depositPlayer(playerName, amount);
  }

  @Override
  public EconomyResponse createBank(String name, String player) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse deleteBank(String name) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse bankBalance(String name) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse bankHas(String name, double amount) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse bankWithdraw(String name, double amount) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse bankDeposit(String name, double amount) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse isBankOwner(String name, String playerName) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public EconomyResponse isBankMember(String name, String playerName) {
    return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
  }

  @Override
  public List<String> getBanks() {
    return List.of();
  }

  @Override
  public boolean createPlayerAccount(String playerName) {
    return true;
  }

  @Override
  public boolean createPlayerAccount(String playerName, String worldName) {
    return true;
  }
}
