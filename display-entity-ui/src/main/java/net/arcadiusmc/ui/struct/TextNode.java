package net.arcadiusmc.ui.struct;

import com.google.common.base.Strings;
import lombok.Getter;
import net.arcadiusmc.ui.render.StringContent;

@Getter
public class TextNode extends Node {

  private String textContent;

  public TextNode(Document owning) {
    super(owning);
  }

  @Override
  public boolean ignoredByMouse() {
    return true;
  }

  public void setTextContent(String textContent) {
    this.textContent = textContent;

    if (Strings.isNullOrEmpty(textContent)) {
      getRenderElement().setContent(null);
    } else {
      getRenderElement().setContent(new StringContent(textContent));
    }

    updateRender();
  }

  @Override
  public void visitorEnter(Visitor visitor) {
    visitor.enterText(this);
  }

  @Override
  public void visitorExit(Visitor visitor) {
    visitor.exitText(this);
  }

  @Override
  public String toString() {
    return "#text";
  }
}
