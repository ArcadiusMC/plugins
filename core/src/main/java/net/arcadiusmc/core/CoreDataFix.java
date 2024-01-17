package net.arcadiusmc.core;

import net.arcadiusmc.datafix.DataUpdaters;

class CoreDataFix {

  static void execute() {
    DataUpdaters updaters = DataUpdaters.create();
    updaters.execute();
  }
}