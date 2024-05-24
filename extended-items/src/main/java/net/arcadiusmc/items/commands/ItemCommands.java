package net.arcadiusmc.items.commands;

public final class ItemCommands {
  private ItemCommands() {}

  public static void registerAll() {
    new CommandCustomItem();
  }
}
