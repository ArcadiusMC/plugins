package net.arcadiusmc.ui.struct;

public class BodyElement extends Element {

  public BodyElement(Document owning) {
    super(owning, Elements.BODY);
  }

  @Override
  void postAlign() {

  }

  @Override
  public void visitorEnter(Visitor visitor) {
    visitor.enterBody(this);
  }

  @Override
  public void visitorExit(Visitor visitor) {
    visitor.exitBody(this);
  }
}
