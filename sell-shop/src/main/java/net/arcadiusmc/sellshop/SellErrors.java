package net.arcadiusmc.sellshop;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRef;

public interface SellErrors {
  MessageRef NO_ITEM_TO_SELL = Messages.reference("sellshop.errors.noItems");
  MessageRef CANNOT_SELL_MORE = Messages.reference("sellshop.cannotSellMore");
}
