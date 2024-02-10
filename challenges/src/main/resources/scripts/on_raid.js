function canComplete(user) {
  return Packages.net.arcadiusmc.guilds.Guilds.getGuild(user) != null;
}

function onEvent(event, handle) {
  event.getWinners().forEach(player => {
    handle.givePoint(player);
  })
}