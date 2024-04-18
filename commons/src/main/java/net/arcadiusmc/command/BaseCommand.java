package net.arcadiusmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.command.help.ArcadiusHelpList;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.AbstractCommand;
import net.forthecrown.grenadier.CommandSource;
import org.bukkit.permissions.Permission;

public abstract class BaseCommand extends AbstractCommand {

  public static final String DEFAULT_DESCRIPTION = "An ArcadiusMC command";

  @Getter
  private boolean simpleUsages = false;

  @Getter @Setter
  private Permission registeredPermission;

  public BaseCommand(String name) {
    super(name.toLowerCase());

    String perm = Commands.getDefaultPermission(getName());

    setPermission(perm);
    setDescription(DEFAULT_DESCRIPTION);

    ArcadiusHelpList helpList = ArcadiusHelpList.helpList();
    helpList.addCommand(this);
  }

  public String getAdminPermission() {
    var perm = getPermission();
    String adminPerm;

    if (perm == null) {
      adminPerm = Commands.getDefaultPermission(getName()) + ".admin";
    } else {
      adminPerm = perm + ".admin";
    }

    Permissions.register(adminPerm);
    return adminPerm;
  }

  public String getHelpListName() {
    return getName();
  }

  public void simpleUsages() {
    simpleUsages = true;
  }

  public void populateUsages(UsageFactory factory) {

  }

  protected static User getUserSender(CommandContext<CommandSource> context)
      throws CommandSyntaxException
  {
    return Users.get(context.getSource().asPlayer());
  }
}