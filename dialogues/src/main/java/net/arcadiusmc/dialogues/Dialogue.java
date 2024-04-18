package net.arcadiusmc.dialogues;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

public class Dialogue {

  private final Map<String, DialogueNode> byName = new Object2ObjectOpenHashMap<>();

  @Getter @Setter
  private DialogueOptions options;

  @Getter @Setter
  long randomId;

  public DialogueNode getEntryPoint() {
    return options.getEntryNode() != null ? getNodeByName(options.getEntryNode()) : null;
  }

  public void addEntry(String name, DialogueNode node) {
    byName.put(name, node);
    node.entry = this;
  }

  public DialogueNode getNodeByName(String name) {
    return byName.get(name);
  }

  public Set<String> getNodeNames() {
    return byName.keySet();
  }

  public static String nodeIdentifier(String dialogueName, String nodeName) {
    return dialogueName + (Strings.isNullOrEmpty(nodeName) ? "" : (";" + nodeName));
  }

  public Collection<DialogueNode> getNodes() {
    return byName.values();
  }
}