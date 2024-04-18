package net.arcadiusmc.dialogues;

import lombok.Getter;
import net.arcadiusmc.dialogues.commands.CommandDialogue;
import net.arcadiusmc.dialogues.commands.CommandDialogueCallback;
import net.arcadiusmc.dialogues.commands.DialogueArgument;
import net.arcadiusmc.dialogues.listeners.ServerListener;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public class DialoguesPlugin extends JavaPlugin {

  @Getter
  private DialogueManager manager;

  public static DialoguesPlugin plugin() {
    return getPlugin(DialoguesPlugin.class);
  }

  @Override
  public void onEnable() {
    manager = new DialogueManager();

    DialogueArgument.setInstance(new DialogueArgument(this));
    new CommandDialogue(this);
    new CommandDialogueCallback(manager);

    Registry<ObjectType<? extends Action>> actions = UsablesPlugin.get().getActions();
    actions.register("dialogue", DialogueAction.TYPE);

    Events.register(new ServerListener(this));
  }
}
