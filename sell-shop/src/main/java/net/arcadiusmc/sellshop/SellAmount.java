package net.arcadiusmc.sellshop;

import lombok.Getter;
import net.arcadiusmc.text.Messages;
import net.kyori.adventure.text.Component;

@Getter
public enum SellAmount {

  PER_1(1),
  PER_16(16),
  PER_64(64),
  ALL(-1);

  private final byte value;

  SellAmount(int i) {
    this.value = (byte)i;
  }

  public int getItemAmount() {
    return Math.max(1, getValue());
  }

  public Component getSellPerText() {
    return Messages.renderText("sellshop.sellAmount.sellPer." + this.name().toLowerCase());
  }

  public Component amountText() {
    return Messages.renderText("sellshop.sellAmount.names." + this.name().toLowerCase());
  }

}