package net.arcadiusmc.usables.conditions;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.items.ItemTestType;
import net.arcadiusmc.usables.scripts.ScriptInstance;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;

public class Conditions {

  public static void registerAll(Registry<ObjectType<? extends Condition>> r) {
    r.register("cooldown", TestCooldown.TYPE);
    r.register("in_world", TestWorld.TYPE);
    r.register("one_use_individual",  TestOneUse.TYPE);
    r.register("one_use_global", TestNeverUsed.TYPE);
    r.register("script_file", ScriptInstance.FILE_TYPE);
    r.register("js", ScriptInstance.RAW_TYPE);
    r.register("not_alt", TestNotAlt.TYPE);
    r.register("inventory_empty", TestEmptyInventory.TYPE);
    r.register("no_riders", NoRiderCondition.TYPE);
    r.register("has_score", TestScore.TYPE);
    r.register("has_items", ItemTestType.CONTAINS);
    r.register("does_not_have_items", ItemTestType.NOT);
    r.register("has_permission", TestPermission.TYPE);
    r.register("hand", HandCondition.TYPE);

    UserService service = Users.getService();
    Registry<Currency> currencies = service.getCurrencies();

    for (Holder<Currency> entry : currencies.entries()) {
      r.register(entry.getKey(), new CurrencyConditionType(entry.getValue()));
    }
  }
}
