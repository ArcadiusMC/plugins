package net.arcadiusmc.usables.expr;

import net.arcadiusmc.usables.objects.Usable;
import net.kyori.adventure.text.Component;

public class ExprList extends Usable {

  @Override
  public String getCommandPrefix() {
    return "";
  }

  @Override
  public Component name() {
    return Component.text("<ExprList>");
  }
}
