package net.arcadiusmc.scripts.modules;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Optional;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.scripts.StringArray;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class WorldsObject implements ProxyObject {

  private Optional<World> getWorld(String key) {
    if (Strings.isNullOrEmpty(key)) {
      return Optional.empty();
    }

    if (key.equalsIgnoreCase("overworld")) {
      return Optional.of(Worlds.overworld());
    }

    if (key.equalsIgnoreCase("nether")) {
      return Optional.of(Worlds.nether());
    }

    if (key.equalsIgnoreCase("end") || key.equalsIgnoreCase("the_end")) {
      return Optional.of(Worlds.end());
    }

    if (key.contains(":")) {
      NamespacedKey rkey = NamespacedKey.fromString(key);

      if (rkey == null) {
        throw Scripts.typeError("Invalid namespaced key: '" + key + "'");
      }

      return Optional.ofNullable(Bukkit.getWorld(rkey));
    }

    return Optional.ofNullable(Bukkit.getWorld(key));
  }

  @Override
  public Object getMember(String key) {
    return getWorld(key).orElse(null);
  }

  @Override
  public Object getMemberKeys() {
    List<World> worlds = Bukkit.getWorlds();
    String[] names = new String[worlds.size()];

    for (int i = 0; i < worlds.size(); i++) {
      names[i] = worlds.get(i).getName();
    }

    return new StringArray(names);
  }

  @Override
  public boolean hasMember(String key) {
    return getWorld(key).isPresent();
  }

  @Override
  public void putMember(String key, Value value) {
    // No op
  }
}
