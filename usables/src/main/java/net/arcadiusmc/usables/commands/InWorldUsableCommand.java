package net.arcadiusmc.usables.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.arcadiusmc.usables.objects.InWorldUsable;
import net.forthecrown.grenadier.CommandSource;

public abstract class InWorldUsableCommand<H extends InWorldUsable> extends InteractableCommand<H> {

  public InWorldUsableCommand(String name) {
    super(name);
  }

  @Override
  protected <B extends ArgumentBuilder<CommandSource, B>> void createEditArguments(
      B argument,
      UsableProvider<H> provider
  ) {
    super.createEditArguments(argument, provider);
    argument.then(vanillaCancelArguments(provider));
  }
}
