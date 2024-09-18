package net.arcadiusmc.cosmetics;

import com.google.common.base.Strings;
import lombok.Getter;
import net.arcadiusmc.user.User;

@Getter
public class CosmeticData<T> {

  private final User user;
  private final CosmeticType<T> type;
  private final ActiveMap activeMap;

  public CosmeticData(User user, CosmeticType<T> type, ActiveMap activeMap) {
    this.user = user;
    this.type = type;
    this.activeMap = activeMap;
  }

  public boolean has(Cosmetic<T> cosmetic) {
    String permission = cosmetic.getPermission();
    if (Strings.isNullOrEmpty(permission)) {
      return true;
    }

    return user.hasPermission(permission);
  }

  public void addAvailable(Cosmetic<T> cosmetic) {
    if (!has(cosmetic)) {
      return;
    }

    String permission = cosmetic.getPermission();
    if (Strings.isNullOrEmpty(permission)) {
      return;
    }

    user.setPermission(permission, true);
  }

  public Cosmetic<T> getActive() {
    return activeMap.getActive(user.getUniqueId(), type);
  }

  public void setActive(Cosmetic<T> cosmetic) {
    activeMap.setActive(user, type, cosmetic);
  }
}
