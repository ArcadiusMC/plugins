package net.arcadiusmc.command.help;

import com.google.common.base.Strings;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.forthecrown.grenadier.CommandSource;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.permissions.Permission;

@Getter
@Accessors(chain = true)
public class Usage implements Predicate<CommandSource> {

  private final String arguments;

  private String[] info;

  @Setter
  private Predicate<CommandSource> condition = commandSource -> true;

  private String permission;

  public Usage(String arguments) {
    this.arguments = arguments;
  }

  public Usage setPermission(Permission permission) {
    this.permission = permission == null ? null : permission.getName();
    return this;
  }

  public Usage setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public Usage addInfo(String info, Object... args) {
    this.info = ArrayUtils.add(this.info, info.formatted(args));
    return this;
  }

  public String argumentsWithPrefix(String prefix) {
    return prefix + (Strings.isNullOrEmpty(arguments) ? "" : " " + arguments);
  }

  @Override
  public boolean test(CommandSource source) {
    if (!Strings.isNullOrEmpty(permission) && !source.hasPermission(permission)) {
      return false;
    }
    if (condition == null) {
      return true;
    }
    return condition.test(source);
  }
}