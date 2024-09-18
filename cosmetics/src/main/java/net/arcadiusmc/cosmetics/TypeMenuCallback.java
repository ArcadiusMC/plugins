package net.arcadiusmc.cosmetics;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import java.util.function.IntSupplier;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.kyori.adventure.audience.Audience;

public class TypeMenuCallback<T> {

  public static CommandSyntaxException notOwned(Audience viewer) {
    return Messages.render("cosmetics.errors.notOwned").exception(viewer);
  }

  public void appendInfo(
      DefaultItemBuilder builder,
      User user,
      Cosmetic<T> cosmetic,
      CosmeticData<T> data
  ) {
    if (data.has(cosmetic)) {
      return;
    }

    builder.addLore(Messages.render("cosmetics.status.notOwned").create(user));
  }

  public void onUnownedClick(User user, Cosmetic<T> cosmetic, CosmeticData<T> data)
      throws CommandSyntaxException
  {
    throw notOwned(user);
  }

  public void onOwnedClick(User user, Cosmetic<T> cosmetic, CosmeticData<T> data)
      throws CommandSyntaxException
  {
    boolean set = Objects.equals(cosmetic, data.getActive());

    if (set) {
      data.setActive(null);

      user.sendMessage(
          Messages.render("cosmetics.unset")
              .addValue("cosmetic", cosmetic.displayName())
              .addValue("type", data.getType().displayName())
              .create(user)
      );
      return;
    }

    data.setActive(cosmetic);

    user.sendMessage(
        Messages.render("cosmetics.set")
            .addValue("cosmetic", cosmetic.displayName())
            .addValue("type", data.getType().displayName())
            .create(user)
    );
  }

  public static class Noop<T> extends TypeMenuCallback<T> {

    @Override
    public void appendInfo(
        DefaultItemBuilder builder,
        User user,
        Cosmetic<T> cosmetic,
        CosmeticData<T> data
    ) {

    }

    @Override
    public void onUnownedClick(User user, Cosmetic<T> cosmetic, CosmeticData<T> data) {
    }

    @Override
    public void onOwnedClick(User user, Cosmetic<T> cosmetic, CosmeticData<T> data) {
    }
  }

  public static class Purchasable<T> extends TypeMenuCallback<T> {

    private final IntSupplier intSupplier;
    private final String currencyName;

    public Purchasable(IntSupplier intSupplier, String currencyName) {
      this.currencyName = currencyName;
      this.intSupplier = intSupplier;
    }

    private int getValue() {
      return intSupplier.getAsInt();
    }

    public Currency getCurrency() {
      return Users.getService().getCurrencies().orThrow(currencyName);
    }

    @Override
    public void appendInfo(
        DefaultItemBuilder builder,
        User user,
        Cosmetic<T> cosmetic,
        CosmeticData<T> data
    ) {
      if (data.has(cosmetic)) {
        return;
      }

      builder.addLore(
          Messages.render("cosmetics.status.price")
              .addValue("value", getCurrency().format(getValue()))
              .create(user)
      );
    }

    @Override
    public void onUnownedClick(User user, Cosmetic<T> cosmetic, CosmeticData<T> data)
        throws CommandSyntaxException
    {
      Currency gems = getCurrency();
      int gemValue = getValue();

      if (user.getGems() <= gemValue) {
        throw Exceptions.cannotAfford(user, gemValue, gems);
      }

      user.setGems(user.getGems() - gemValue);
      data.addAvailable(cosmetic);

      user.sendMessage(
          Messages.render("cosmetics.bought")
              .addValue("cosmetic", cosmetic.displayName())
              .addValue("value", gems.format(gemValue))
              .create(user)
      );
    }
  }
}
