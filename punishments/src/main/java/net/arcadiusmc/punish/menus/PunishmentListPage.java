package net.arcadiusmc.punish.menus;

import static net.arcadiusmc.punish.menus.AdminUi.ENTRY;
import static net.arcadiusmc.punish.menus.AdminUi.HEADER;
import static net.arcadiusmc.punish.menus.AdminUi.PAGE;
import static net.arcadiusmc.punish.menus.AdminUi.PUNISHMENT;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.page.ListPage;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishment;
import net.arcadiusmc.punish.commands.PunishCommands;
import net.arcadiusmc.text.BufferedTextWriter;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class PunishmentListPage extends ListPage<Punishment> {

  private final boolean current;

  public PunishmentListPage(MenuPage parent, boolean current) {
    super(parent, PAGE);
    this.current = current;

    initMenu(
        Menus.builder(Component.text((current ? "Current" : "Past") + " punishments"))
            .setSize(Menus.sizeFromRows(4)),

        true
    );
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    var builder = ItemStacks.builder(Material.CHEST)
        .setName((current ? "Current" : "Past") + " punishments");

    if (current) {
      builder.addLore("&7Currently active punishments");
    } else {
      builder.addLore("&7Punishments this user has")
          .addLore("&7been given in the past");
    }

    if (getList(user, context).isEmpty()) {
      builder.addLore("&cNo entries to show");
    }

    return builder.build();
  }

  @Override
  protected List<Punishment> getList(User user, Context ctx) {
    var entry = ctx.getOrThrow(ENTRY);
    return current ? entry.getCurrent() : entry.getPast();
  }

  @Override
  protected ItemStack getItem(User user, Punishment punish, Context ctx) {
    return createItem(user, punish, current);
  }

  static ItemStack createItem(User user, Punishment punish, boolean current) {
    DefaultItemBuilder builder = ItemStacks.builder(typeToMaterial(punish.getType()))
        .addFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ATTRIBUTES);

    builder.setName(
        Messages.render("punishMenu." + (current ? "active" : "past"))
            .addValue("punishments", punish.getType().presentableName())
            .create(user)
    );

    BufferedTextWriter writer = TextWriters.buffered();
    writer.setFieldValueStyle(Style.style(NamedTextColor.GRAY));
    punish.writeDisplay(writer);

    builder.addLoreRaw(writer.getBuffer());

    return builder.build();
  }

  static Material typeToMaterial(PunishType type) {
    return switch (type) {
      case BAN -> Material.IRON_AXE;
      case JAIL -> Material.IRON_BARS;
      case MUTE -> Material.BARRIER;
      case SOFTMUTE -> Material.STRUCTURE_VOID;
      case IPBAN -> Material.DIAMOND_AXE;
      case KICK -> Material.NETHERITE_BOOTS;
    };
  }

  // On entry click
  @Override
  protected void onClick(User user, Punishment entry, Context context, ClickContext click)
      throws CommandSyntaxException
  {
    if (!current) {
      return;
    }

    User target = context.getOrThrow(ENTRY).getUser();
    PunishCommands.ensureCanPardon(user.getCommandSource(), target, entry.getType());

    var node = new PardonPage(this, entry);
    node.getMenu().open(user, context);
  }

  // On page open
  @Override
  public void onClick(User user, Context context, ClickContext click)
      throws CommandSyntaxException
  {
    super.onClick(user, context, click);

    if (current) {
      context.set(PUNISHMENT, null);
    }
  }

  @Override
  protected MenuNode createHeader() {
    return HEADER;
  }
}