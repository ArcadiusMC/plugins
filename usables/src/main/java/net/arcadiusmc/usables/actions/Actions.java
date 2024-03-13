package net.arcadiusmc.usables.actions;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.items.ItemActionType;
import net.arcadiusmc.usables.scripts.ScriptInstance;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;

public class Actions {

  public static void registerAll(Registry<ObjectType<? extends Action>> r) {
    r.register("script_file", ScriptInstance.FILE_TYPE);
    r.register("js", ScriptInstance.RAW_TYPE);
    r.register("console_cmd", CommandAction.AS_SELF);
    r.register("player_cmd", CommandAction.AS_PLAYER);
    r.register("show_text", TextAction.TYPE);
    r.register("give_items", ItemActionType.GIVE);
    r.register("take_items", ItemActionType.TAKE);
    r.register("teleport", TeleportAction.TYPE);
    r.register("play_sound", SoundAction.TYPE);

    UserService service = Users.getService();
    Registry<Currency> currencies = service.getCurrencies();

    for (Holder<Currency> entry : currencies.entries()) {
      CurrencyActions.registerAll(r, entry.getKey(), entry.getValue());
    }

    ScoreActions.registerAll(r);
  }
}
