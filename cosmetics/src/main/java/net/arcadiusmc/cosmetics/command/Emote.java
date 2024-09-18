package net.arcadiusmc.cosmetics.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import net.arcadiusmc.Cooldowns;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.cosmetics.CosmeticsSettings;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.permissions.Permission;

public abstract class Emote extends BaseCommand {

  public static final Permission PERMISSION = Permissions.register("arcadius.cosmetics.emotes");

  final String messageKey;

  public Emote(String name) {
    this(name, name);
  }

  public Emote(String name, String messageKey) {
    super(name);

    this.messageKey = messageKey;
    setPermission(Permissions.register(PERMISSION, name));
  }

  public abstract void emoteSelf(User user);

  public abstract void emote(User sender, User target) throws CommandSyntaxException;

  protected Duration cooldownDuration() {
    return Duration.ofSeconds(3);
  }

  protected final void sendSelfMessage(User user) {
    Component message = Messages.render("cosmetics.emotes", messageKey, "self").create(user);
    user.sendMessage(message);
  }

  protected final void sendMessages(User sender, User target) {
    Component senderMessage = Messages.render("cosmetics.emotes", messageKey, "sender")
        .addValue("sender", sender)
        .addValue("target", target)
        .create(sender)
        .clickEvent(ClickEvent.runCommand("/" + getName() + " " + target.getName()));

    Component targetMessage = Messages.render("cosmetics.emotes", messageKey, "target")
        .addValue("sender", sender)
        .addValue("target", target)
        .create(target);

    if (canUse(target.getCommandSource())) {
      targetMessage = targetMessage
          .clickEvent(ClickEvent.runCommand("/" + getName() + " " + sender.getName()))
          .hoverEvent(
              Messages.render("cosmetics.emotes", messageKey, "target.hover")
                  .addValue("sender", sender)
                  .addValue("target", target)
                  .create(target)
          );
    }

    sender.sendMessage(senderMessage);
    target.sendMessage(targetMessage);
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          emoteSelf(user);
          return 0;
        })

        .then(argument("target", Arguments.ONLINE_USER)
            .executes(c -> {
              User sender = getUserSender(c);
              User target = Arguments.getUser(c, "target");

              if (sender.equals(target)) {
                emoteSelf(sender);
                return 0;
              }

              if (!sender.get(CosmeticsSettings.EMOTES_ENABLED)) {
                throw Messages.render("cosmetics.errors.emotesSelf")
                    .addValue("sender", sender)
                    .addValue("target", target)
                    .exception(sender);
              }
              if (!target.get(CosmeticsSettings.EMOTES_ENABLED)) {
                throw Messages.render("cosmetics.errors.emotesTarget")
                    .addValue("sender", sender)
                    .addValue("target", target)
                    .exception(sender);
              }

              Cooldowns cooldowns = Cooldowns.cooldowns();
              if (cooldowns.onCooldown(sender.getUniqueId(), getPermission())) {
                throw Messages.render("cosmetics.emotes", messageKey, "cooldown")
                    .addValue("sender", sender)
                    .addValue("target", target)
                    .exception(sender);
              }

              emote(sender, target);

              cooldowns.cooldown(sender.getUniqueId(), getPermission(), cooldownDuration());
              return 0;
            })
        );
  }
}
