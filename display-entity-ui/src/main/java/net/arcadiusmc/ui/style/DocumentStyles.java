package net.arcadiusmc.ui.style;

import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.event.EventListener;
import net.arcadiusmc.ui.event.EventTypes;
import net.arcadiusmc.ui.event.MouseEvent;
import net.arcadiusmc.ui.event.MutationEvent;
import net.arcadiusmc.ui.event.MutationEvent.Action;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.struct.Attr;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.struct.Element;
import net.arcadiusmc.ui.struct.Node;
import net.arcadiusmc.ui.style.Rule.StyleFunction;
import net.arcadiusmc.ui.style.StylePropertyMap.DifferenceIterator;
import net.arcadiusmc.ui.style.StylePropertyMap.RuleIterator;
import net.arcadiusmc.ui.style.Stylesheet.Style;

public class DocumentStyles {

  private final List<Style> styles = new ArrayList<>();
  private final Map<Node, StylePropertyMap> inlineStyleMap = new HashMap<>();

  private final Document document;

  public DocumentStyles(Document document) {
    this.document = document;
  }

  public void init() {
    InputListener inputListener = new InputListener();
    document.addEventListener(EventTypes.MOUSE_DOWN, inputListener);
    document.addEventListener(EventTypes.MOUSE_ENTER, inputListener);
    document.addEventListener(EventTypes.MOUSE_LEAVE, inputListener);
    document.addEventListener(EventTypes.CLICK_EXPIRE, inputListener);

    MutationListener mutationListener = new MutationListener();
    document.addEventListener(EventTypes.APPEND_CHILD, mutationListener);
    document.addEventListener(EventTypes.MODIFY_ATTR, mutationListener);
  }

  public void addStylesheet(Stylesheet stylesheet) {
    styles.addAll(stylesheet.getStyles());
    styles.sort(Comparator.naturalOrder());

    Element body = document.getBody();
    if (body != null) {
      recursivelyApplyStyles(body, null);
      body.align();
    }
  }

  private void applyCascading(StylePropertyMap target, Node node) {
    Node parent = node.getParent();

    if (parent == null) {
      return;
    }

    StylePropertyMap parentSet = parent.getRenderElement().getStyleProperties();
    RuleIterator it = parentSet.iterator();

    while (it.hasNext()) {
      it.next();

      Rule<Object> rule = it.rule();

      if (!rule.isCascading()) {
        continue;
      }

      Object o = it.value();

      if (target.has(rule)) {
        continue;
      }

      target.set(rule, o);
    }
  }

  public void recursivelyApplyStyles(Node node, ChangeSet allChanges) {
    RenderElement re = node.getRenderElement();

    StylePropertyMap set = re.getStyleProperties();
    StylePropertyMap newSet = new StylePropertyMap();
    StylePropertyMap old = new StylePropertyMap();
    old.putAll(set);

    applyCascading(newSet, node);

    if (node instanceof Element e) {
      for (Style style : styles) {
        if (!style.selector().test(e)) {
          continue;
        }

        newSet.putAll(style.rules());
      }

      StylePropertyMap inline = inlineStyleMap.get(e);
      if (inline != null) {
        newSet.putAll(inline);
      }
    }

    int changes = detectChange(old, newSet);

    if (changes != 0) {
      int cmask = StyleChange.CONTENT.mask;

      // If it contains the CONTENT style change
      if ((changes & cmask) == cmask) {
        re.setContentDirty(true);
      }

      applyRules(node, newSet);
    }

    if (allChanges != null) {
      allChanges.changes |= changes;
    }

    for (Node child : node.getChildren()) {
      recursivelyApplyStyles(child, allChanges);
    }
  }

  private int detectChange(StylePropertyMap old, StylePropertyMap newSet) {
    DifferenceIterator dif = old.difference(newSet);
    int result = 0;

    while (dif.hasNext()) {
      dif.next();

      if (dif.rule().isLayoutAffecting()) {
        result |= StyleChange.LAYOUT.mask;
      }

      if (dif.rule().isContentAffecting()) {
        result |= StyleChange.CONTENT.mask;
      }

      result |= StyleChange.VISUAL.mask;
    }

    return result;
  }

  private void applyRules(Node node, StylePropertyMap set) {
    StylePropertyMap nodeSet = node.getRenderElement().getStyleProperties();
    nodeSet.clear();
    nodeSet.putAll(set);

    Screen screen = document.getScreen();

    for (Rule<?> rule : Rules.REGISTRY) {
      Object value = set.get(rule);
      StyleFunction<Object> func = (StyleFunction<Object>) rule.getApplicator();

      if (func == null) {
        continue;
      }

      func.apply(node, screen, value);
    }
  }

  void applyChanges(ChangeSet set) {
    Element body = document.getBody();

    if (body == null) {
      return;
    }

    if (set.contains(StyleChange.CONTENT)) {
      return;
    }
  }

  class InputListener implements EventListener.Typed<MouseEvent> {

    @Override
    public void onEventFired(MouseEvent event) {
      ChangeSet set = new ChangeSet();
      recursivelyApplyStyles(event.getTarget(), set);

      Element body = document.getBody();

      if (body == null) {
        return;
      }

      if (set.contains(StyleChange.CONTENT)) {
        body.spawn();
      }

      if (set.contains(StyleChange.LAYOUT)) {
        body.align();
      }
    }
  }

  class MutationListener implements EventListener.Typed<MutationEvent> {

    static final Set<Action> ACCEPTED = Set.of(
        Action.ADD_ATTR,
        Action.SET_ATTR,
        Action.REMOVE_ATTR
    );

    @Override
    public void onEventFired(MutationEvent event) {
      Action action = event.getAction();

      if (!ACCEPTED.contains(action)) {
        return;
      }

      Node node = event.getNode();
      StylePropertyMap info = inlineStyleMap.get(node);

      if (event.getAttrName().equals(Attr.STYLE)) {
        boolean anythingChanged = reconfigureInline(node, info, action, event.getNewValue());

        if (!anythingChanged) {
          return;
        }
      }

      ChangeSet set = new ChangeSet();
      recursivelyApplyStyles(node, set);

      Element body = document.getBody();

      if (body == null) {
        return;
      }

      if (document.getBody() != null && set.contains(StyleChange.LAYOUT)) {
        document.getBody().align();
      }
    }

    boolean reconfigureInline(Node node, StylePropertyMap inline, Action action, String value) {
      if (action == Action.REMOVE_ATTR) {
        if (inline == null) {
          return false;
        }

        inline.clear();
        return true;
      }

      if (inline == null) {
        inline = new StylePropertyMap();
        inlineStyleMap.put(node, inline);
      }

      StylePropertyMap finalInline = inline;

      DataResult<StylePropertyMap> result = Styles.parseInlineStyle(value)
          .ifError(ruleSetError -> {
            Loggers.getLogger().error(ruleSetError.message());
          })
          .ifSuccess(finalInline::putAll);

      return result.isSuccess();
    }

  }

  public enum StyleChange {
    VISUAL, // Only visual changes
    CONTENT, // Content was somehow mutated
    LAYOUT // Forces re-align
    ;

    final int mask;

    StyleChange() {
      this.mask = 1 << ordinal();
    }
  }

  static class ChangeSet {
    private int changes = 0;

    boolean contains(StyleChange change) {
      return (changes & change.mask) == change.mask;
    }

    void add(StyleChange change) {
      changes |= change.mask;
    }

    boolean isEmpty() {
      return changes == 0;
    }
  }
}
