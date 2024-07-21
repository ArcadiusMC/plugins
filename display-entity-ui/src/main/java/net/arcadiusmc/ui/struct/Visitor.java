package net.arcadiusmc.ui.struct;

public interface Visitor {

  default void visit(Node n) {
    n.visitorEnter(this);

    for (Node child : n.getChildren()) {
      visit(child);
    }

    n.visitorExit(this);
  }

  void enterElement(Element element);

  void exitElement(Element element);

  void enterText(TextNode node);

  void exitText(TextNode node);

  default void enterItem(ItemElement element) {
    enterElement(element);
  }

  default void exitItem(ItemElement element) {
    exitElement(element);
  }

  default void enterBody(BodyElement element) {
    enterElement(element);
  }

  default void exitBody(BodyElement element) {
    exitElement(element);
  }

  default void enterButton(ButtonElement element) {
    enterElement(element);
  }

  default void exitButton(ButtonElement element) {
    exitElement(element);
  }
}
