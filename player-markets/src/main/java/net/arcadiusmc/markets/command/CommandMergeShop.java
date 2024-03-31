package net.arcadiusmc.markets.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import java.util.UUID;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.request.PlayerRequest;
import net.arcadiusmc.command.request.RequestTable;
import net.arcadiusmc.command.request.RequestValidator;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;
import net.arcadiusmc.utils.Audiences;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.commons.lang3.function.FailableConsumer;

public class CommandMergeShop extends BaseCommand {

  private final RequestTable<MergeRequest> requests;

  public CommandMergeShop() {
    super("mergeshop");

    requests = new RequestTable<>();
    requests.setValidator(Validator.VALIDATOR);

    setDescription("Lets you merge your shop with someone else's");
    setAliases("merge-shop");

    register();
  }

  private LiteralArgumentBuilder<CommandSource> arg(
      boolean incoming,
      String argument,
      FailableConsumer<MergeRequest, CommandSyntaxException> consumer
  ) {
    String errorKey = incoming ? "noIncoming" : "noOutgoing";

    return literal(argument)
        .executes(c -> {
          User user = getUserSender(c);

          MergeRequest request = incoming
              ? requests.latestIncoming(user)
              : requests.latestOutgoing(user);

          if (request == null) {
            throw Messages.render("markets.errors.merges", errorKey)
                .exception(user);
          }

          consumer.accept(request);
          return 0;
        })

        .then(argument("user", Arguments.ONLINE_USER)
            .executes(c -> {
              User user = getUserSender(c);
              User other = Arguments.getUser(c, "user");

              MergeRequest request = incoming
                  ? requests.getIncoming(user, other)
                  : requests.getOutgoing(user, other);

              if (request == null) {
                throw Messages.render("markets.errors.merges", errorKey, "player")
                    .addValue("player", other)
                    .exception(user);
              }

              consumer.accept(request);
              return 0;
            })
        );
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.ONLINE_USER)
            // mergeshop <user>
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              MergeRequest request = new MergeRequest(user.getUniqueId(), target.getUniqueId());
              requests.sendRequest(request);

              return 0;
            })
        )

        // /mergeshop accept <player>
        .then(arg(true, "accept", PlayerRequest::accept))

        // /mergeshop deny <player>
        .then(arg(true, "deny", PlayerRequest::deny))

        // /mergeshop cancel <player>
        .then(arg(false, "cancel", PlayerRequest::cancel));
  }

  enum Validator implements RequestValidator<MergeRequest> {
    VALIDATOR;

    @Override
    public void validate(MergeRequest request, Audience viewer) throws CommandSyntaxException {
      if (request.getTargetId().equals(request.getSenderId())) {
        throw Messages.render("markets.errors.merges.self")
            .exception(viewer);
      }

      boolean viewerIsTarget = Audiences.equals(viewer, request.getTarget());

      User viewUser = viewerIsTarget ? request.getTarget() : request.getSender();
      User target   = viewerIsTarget ? request.getSender() : request.getTarget();

      Market viewerShop = Markets.getOwned(viewUser);
      Market targetShop = Markets.getOwned(target);

      if (viewerShop == null) {
        throw MExceptions.noMarketOwned(viewer);
      }

      if (targetShop == null) {
        throw MExceptions.ownsNoMarket(viewer, target);
      }

      if (viewerShop.isConnected(targetShop)) {
        throw Messages.render("markets.errors.merges.notConnected")
            .addValue("player", target)
            .exception(viewer);
      }

      if (viewerShop.getMerged() != null) {
        throw Messages.render("markets.errors.merges.alreadyIs.self")
            .exception(viewer);
      }

      if (targetShop.getMerged() != null) {
        throw Messages.render("markets.errors.merges.alreadyIs.target")
            .addValue("player", target)
            .exception(viewer);
      }

      UserBlockList.testBlockedException(
          viewUser,
          target,
          Messages.reference("markets.errors.merges.blocked.self"),
          Messages.reference("markets.errors.merges.blocked.target")
      );
    }
  }

  static class MergeRequest extends PlayerRequest {

    public MergeRequest(UUID senderId, UUID targetId) {
      super(senderId, targetId);
    }

    @Override
    protected Duration getExpiryDuration() {
      return Duration.ofMinutes(5);
    }

    @Override
    public void onBegin() {
      User sender = getSender();
      User target = getTarget();

      sender.sendMessage(
          Messages.render("markets.merge.sent.sender")
              .addValue("sender", sender)
              .addValue("target", target)
              .addValue("cancel",
                  Messages.render("markets.merge.cancel")
                      .create(sender)
                      .clickEvent(ClickEvent.runCommand("/merge-shop cancel " + target.getName()))
              )
              .create(sender)
      );

      target.sendMessage(
          Messages.render("markets.merge.sent.target")
              .addValue("sender", sender)
              .addValue("target", target)

              .addValue("accept",
                  Messages.BUTTON_ACCEPT_TICK.renderText(target)
                      .clickEvent(ClickEvent.runCommand("/merge-shop accept " + sender.getName()))
              )

              .addValue("deny",
                  Messages.BUTTON_DENY_CROSS.renderText(target)
                      .clickEvent(ClickEvent.runCommand("/merge-shop deny " + sender.getName()))
              )

              .create(target)
      );

    }

    Component mergedWith(Audience viewer, User with) {
      return Messages.render("markets.merged")
          .addValue("player", with)
          .create(viewer);
    }

    @Override
    public void accept() throws CommandSyntaxException {
      super.accept();

      User sender = getSender();
      User target = getTarget();

      Market senderShop = Markets.getOwned(sender);
      Market targetShop = Markets.getOwned(target);

      senderShop.merge(targetShop);

      sender.sendMessage(mergedWith(sender, target));
      target.sendMessage(mergedWith(target, sender));

      stop();
    }

    @Override
    public void deny() {
      stop();
      sendMessages("denied");
    }

    @Override
    public void cancel() {
      stop();
      sendMessages("cancelled");
    }

    void sendMessages(String key) {
      User sender = getSender();
      User target = getTarget();

      sender.sendMessage(
          Messages.render("markets.merge", key, "sender")
              .addValue("sender", sender)
              .addValue("target", target)
              .create(sender)
      );

      target.sendMessage(
          Messages.render("markets.merge", key, "target")
              .addValue("sender", sender)
              .addValue("target", target)
              .create(target)
      );

    }
  }
}
