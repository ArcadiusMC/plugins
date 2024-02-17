package net.arcadiusmc.signshops;

import lombok.Getter;
import net.arcadiusmc.text.Messages;
import net.kyori.adventure.text.Component;

/**
 * Represents a shop's type
 */
public enum ShopType {

  /* ----------------------------- SELL TYPES ------------------------------ */

  /**
   * The regular sell type where a player gives a shop items in exchange for currency
   */
  SELL(Interactions.SELL),

  /**
   * A sell type where the inventory of the shop doesn't change and can be sold to forever...
   * <p>
   * because the server just prints the money like the federal reserve... sorry
   */
  ADMIN_SELL(Interactions.ADMIN_SELL),

  /* ----------------------------- BUY TYPES ------------------------------ */

  /**
   * Shop type where a player will give the shop Rhines in exchange for items
   */
  BUY(Interactions.BUY),

  /**
   * Shop type where the shop never runs out of materials and to sell to the player
   */
  ADMIN_BUY(Interactions.ADMIN_BUY);

  /* ----------------------------- INSTANCE FIELDS ------------------------------ */

  /**
   * Determines if this type is an admin type or not
   */
  @Getter
  private final boolean admin;

  /**
   * Determines if this type buys from their customer or sells to them
   */
  @Getter
  private final boolean buyType;

  /**
   * The interaction shops of this type will have with customers
   */
  @Getter
  private final ShopInteraction interaction;

  /* ----------------------------------------------------------- */

  ShopType(ShopInteraction interaction) {
    // Auto-detect buy type and if this is an admin type
    this.buyType = name().contains("BUY");
    this.admin = name().contains("ADMIN");
    this.interaction = interaction;
  }

  public Component getStockedLabel() {
    return Messages.renderText("signshops.labels." + name().toLowerCase(), null);
  }

  public Component getUnStockedLabel() {
    return Messages.renderText("signshops.labels." + name().toLowerCase() + ".noStock", null);
  }

  public ShopType toAdmin() {
    return switch (this) {
      case BUY -> ADMIN_BUY;
      case SELL -> ADMIN_SELL;

      // This is an admin type
      default -> this;
    };
  }
}