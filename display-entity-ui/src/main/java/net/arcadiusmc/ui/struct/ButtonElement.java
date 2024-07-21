package net.arcadiusmc.ui.struct;

public class ButtonElement extends Element {

  public ButtonElement(Document owning) {
    super(owning, Elements.BUTTON);
  }

  public boolean isEnabled() {
    return getBooleanAttribute(Attr.ENABLED, true);
  }

  public void setEnabled(boolean value) {
    setAttribute(Attr.ENABLED, String.valueOf(value));
  }

  @Override
  public void visitorEnter(Visitor visitor) {
    visitor.enterButton(this);
  }

  @Override
  public void visitorExit(Visitor visitor) {
    visitor.exitButton(this);
  }
}
