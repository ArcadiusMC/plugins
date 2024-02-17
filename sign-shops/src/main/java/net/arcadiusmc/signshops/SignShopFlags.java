package net.arcadiusmc.signshops;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.arcadiusmc.Loggers;

public class SignShopFlags {

  public static final StateFlag SHOP_CREATION = new StateFlag("shop-creation", true);
  public static final StateFlag MEMBER_EDITING = new StateFlag("member-can-edit-signshops", false);

  public static void register(FlagRegistry registry) {
    try {
      registry.register(SHOP_CREATION);
      registry.register(MEMBER_EDITING);
    } catch (FlagConflictException exc) {
      Loggers.getLogger().error("Error registering flag {}: {}",
          SHOP_CREATION.getName(), exc.getMessage()
      );
    }
  }
}
