package net.arcadiusmc.sellshop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.Getter;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import org.bukkit.Material;

/**
 * The result of a {@link ItemSell#sell()} call.
 * <p>
 * Contains all the necessary data of the {@link ItemSell} to perform selling operations as well as
 * allowing the item sell pass in a {@link CommandSyntaxException} if it failed to sell items at any
 * point.
 */
@Getter
public class SellResult {

  /**
   * The sold amount of items
   */
  private final int sold;

  /**
   * The amount of rhines earned from selling
   */
  private final int earned;

  /**
   * The target amount of items to sell
   */
  private final int target;

  /**
   * A potential failure exception thrown in the selling process, may be null, if the selling was
   * successful
   */
  private final MessageRef failure;

  public SellResult(ItemSell sell, MessageRef failure) {
    this.sold = sell.getSold();
    this.earned = sell.getEarned();
    this.target = sell.getTarget();

    this.failure = failure;
  }

  public SellResult(int sold, int earned, int target, MessageRef failure) {
    this.sold = sold;
    this.earned = earned;
    this.target = target;
    this.failure = failure;
  }

  /**
   * Creates a failed sell result, which failed due to a lack of items
   *
   * @param sell The item sell instance
   * @return The created result
   */
  public static SellResult notEnoughItems(ItemSell sell) {
    return new SellResult(sell, SellErrors.NO_ITEM_TO_SELL);
  }

  /**
   * Creates a result that failed due to the item price dropping to 0
   *
   * @param sell The item sell instance
   * @return The created result
   */
  public static SellResult cannotSellMore(ItemSell sell) {
    return new SellResult(sell, SellErrors.CANNOT_SELL_MORE);
  }

  /**
   * Creates a successful sell result
   *
   * @param sell The item sell instance
   * @return The created result
   */
  public static SellResult success(ItemSell sell) {
    return new SellResult(sell, null);
  }

  public SellResult callModifyEvent(User user, List<String> tags, Material material) {
    if (failure != null) {
      return this;
    }

    return SellShop.applyCalculationEvent(this, user, tags, material);
  }

  public SellResult modify(UnaryOperator<SellResult> operator) {
    if (failure != null) {
      return this;
    }

    return operator.apply(this);
  }
}