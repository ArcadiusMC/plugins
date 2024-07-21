package net.arcadiusmc.ui.style.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.ui.struct.Element;
import net.arcadiusmc.ui.struct.Node;
import org.jetbrains.annotations.NotNull;

public class Selector implements Comparable<Selector> {

  final SelectorNode[] nodes;

  @Getter
  final Spec specificity;

  public Selector(SelectorNode[] nodes) {
    Objects.requireNonNull(nodes, "Null nodes");
    this.nodes = nodes;
    this.specificity = calculateSpecificity();
  }

  private Spec calculateSpecificity() {
    Spec spec = new Spec();

    for (SelectorNode node : nodes) {
      node.appendSpecificity(spec);
    }

    return spec;
  }

  public boolean isEmpty() {
    return nodes.length < 1;
  }

  public boolean test(Node node) {
    if (isEmpty()) {
      return true;
    }

    int last = nodes.length - 1;
    Node p = node;

    for (int i = last; i >= 0; i--) {
      SelectorNode filter = nodes[i];

      if (!(p instanceof Element e) || !filter.test(e)) {
        return false;
      }

      p = p.getParent();
    }

    return true;
  }

  public Node locateFirst(Node node) {
    List<Node> list = locateAll(node);

    if (list.isEmpty()) {
      return null;
    }

    return list.getFirst();
  }

  public List<Node> locateAll(Node node) {
    List<Node> out = new ArrayList<>();

    if (!isEmpty()) {
      recursiveAdd(node, 0, out);
    }

    return out;
  }

  private void recursiveAdd(Node node, int index, List<Node> out) {
    if (!(node instanceof Element e)) {
      return;
    }

    SelectorNode n = nodes[index];

    if (!n.test(e)) {
      return;
    }

    if (index == (nodes.length - 1)) {
      out.add(node);
      return;
    }

    for (Node child : node.getChildren()) {
      recursiveAdd(child, index + 1, out);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < nodes.length; i++) {
      if (i != 0) {
        builder.append(' ');
      }

      nodes[i].append(builder);
    }

    builder.append(' ').append(specificity);

    return builder.toString();
  }

  @Override
  public int compareTo(@NotNull Selector o) {
    return specificity.compareTo(o.specificity);
  }
}
