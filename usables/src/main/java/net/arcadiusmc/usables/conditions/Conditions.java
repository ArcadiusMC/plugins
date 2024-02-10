package net.arcadiusmc.usables.conditions;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.scripts.ScriptInstance;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;

public class Conditions {

  public static void registerAll(Registry<ObjectType<? extends Condition>> r) {
    r.register("cooldown", TestCooldown.TYPE);
    r.register("in_world", TestWorld.TYPE);
    r.register("one_use",  TestOneUse.TYPE);
    r.register("never_used", TestNeverUsed.TYPE);
    r.register("test_script", ScriptInstance.TYPE);
    r.register("not_alt", TestNotAlt.TYPE);
    r.register("inventory_empty", TestEmptyInventory.TYPE);
    r.register("no_riders", NoRiderCondition.TYPE);
    r.register("has_score", TestScore.TYPE);
    r.register("has_items", ItemCondition.HAS_ITEMS);
    r.register("does_not_have_items", ItemCondition.MISSING_ITEMS);
    r.register("has_permission", TestPermission.TYPE);
    r.register("hand", HandCondition.TYPE);

    UserService service = Users.getService();
    Registry<Currency> currencies = service.getCurrencies();

    for (Holder<Currency> entry : currencies.entries()) {
      r.register(entry.getKey(), new CurrencyConditionType(entry.getValue()));
    }
  }
}
