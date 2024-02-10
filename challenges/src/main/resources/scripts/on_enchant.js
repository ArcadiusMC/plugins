function canComplete(user) {
  return Packages.net.arcadiusmc.guilds.Guilds.getGuild(user) != null;
}

function onEvent(event, handle) {
  // EnchantItemEvent
  if (event.getExpLevelCost() >= 30) {
    handle.givePoint(event.getEnchanter());
  }
}