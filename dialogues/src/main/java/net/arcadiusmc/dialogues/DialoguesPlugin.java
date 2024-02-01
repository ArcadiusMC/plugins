package net.arcadiusmc.dialogues;

import lombok.Getter;
import net.arcadiusmc.dialogues.commands.CommandDialogue;
import net.arcadiusmc.dialogues.commands.DialogueArgument;
import org.bukkit.plugin.java.JavaPlugin;

public class DialoguesPlugin extends JavaPlugin {

  @Getter
  private DialogueManager manager;

  @Override
  public void onEnable() {
    manager = new DialogueManager();
    manager.load();
    DialogueArgument.setInstance(new DialogueArgument(this));
    new CommandDialogue(this);
  }
}
