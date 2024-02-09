function canComplete(user) {
  return Packages.net.arcadiusmc.guilds.Guilds.getGuild(user) != null;
}

function onEvent(event, handle) {
  // "Custom" event should be triggered when selling items in /shop
  // (points = Rhines earned)
  handle.givePoints(event.getUser(), event.getEarned());
}