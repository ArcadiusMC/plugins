package net.arcadiusmc.ui.struct;

import io.papermc.paper.inventory.tooltip.TooltipContext;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.ui.render.ComponentContent;
import net.arcadiusmc.ui.render.ItemContent;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

@Getter
public class ItemElement extends Element {

  private ItemStack item;

  private Node itemTooltip;

  public ItemElement(Document owning) {
    super(owning, Elements.ITEM);
  }

  @Override
  public Node getTooltip() {
    Node superResult = super.getTooltip();

    if (superResult != null) {
      return superResult;
    }

    if (itemTooltip == null || getBooleanAttribute(Attr.HIDE, false)) {
      return null;
    }

    return itemTooltip;
  }

  public void setItem(ItemStack item) {
    this.item = item;

    if (ItemStacks.isEmpty(item)) {
      getRenderElement().setContent(null);
      itemTooltip = null;
    } else {
      ItemContent content = new ItemContent(item);
      getRenderElement().setContent(content);
      createTooltip();
    }
  }

  void createTooltip() {
    List<Component> list = new ArrayList<>();
    list.addAll(item.computeTooltipLines(TooltipContext.create(false, false), null));

    Document doc = getOwning();
    Element container = doc.createElement(Elements.ITEM_TOOLTIP);

    if (!list.isEmpty()) {
      Element nameElem = doc.createElement(Elements.ITEM_TOOLTIP_NAME);
      nameElem.getRenderElement().setContent(new ComponentContent(list.getFirst()));

      container.addChild(nameElem);
      list.removeFirst();
    }

    for (Component component : list) {
      // "empty" components are treated as being size 0 by 0, but
      // we just need an empty line so use a space to avoid that 0 by 0 size issue.
      if (Text.isEmpty(component)) {
        component = Component.space();
      }

      Element line = doc.createElement(Elements.ITEM_TOOLTIP_LINE);
      line.getRenderElement().setContent(new ComponentContent(component));
      container.addChild(line);
    }

    container.setDepth(getDepth());
    this.itemTooltip = container;
  }

  @Override
  public void visitorEnter(Visitor visitor) {
    visitor.enterItem(this);
  }

  @Override
  public void visitorExit(Visitor visitor) {
    visitor.exitItem(this);
  }
}
