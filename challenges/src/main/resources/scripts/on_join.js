function canComplete(user) {
  return Packages.net.arcadiusmc.guilds.Guilds.getGuild(user) != null;
}

function onActivate(handle) {
  Users.getOnline().forEach(user => {
    handle.givePoint(user);
  });
}