package net.arcadiusmc.core.commands.item;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.chat.MessageSuggestions;
import net.arcadiusmc.command.help.Usage;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.commands.CommandNickname;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameNode extends ItemModifierNode {

  private final NameAccess access;

  public ItemNameNode(NameAccess access) {
    super(access.commandName, access.aliases);
    this.access = access;
  }

  @Override
  String getArgumentName() {
    return access.argumentName;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    namingNote(
        factory.usage("<text>")
            .addInfo("Sets the %s of the item you're holding", access.argumentName)
    );

    factory.usage("-clear")
        .addInfo("Clears the %s of the item you're holding", access.argumentName);
  }

  static void namingNote(Usage usage) {
    usage.addInfo("Note:")
        .addInfo("If the <text> is a JSON component (eg: {\"text\":\"Item Name\"})")
        .addInfo("The name won't automatically become non-italic")
        .addInfo("and white, you'll be required to manually set them to")
        .addInfo("that configuration");
  }

  @Override
  public void create(LiteralArgumentBuilder<CommandSource> command) {
    command
        .then(argument("name", Arguments.CHAT)
            .suggests((context, builder) -> {
              return MessageSuggestions.get(context, builder, true, (builder1, source) -> {
                Completions.suggest(builder1, CommandNickname.CLEAR);

                // Suggest existing item name
                getItemSuggestions(source, itemStack -> {
                  Component name = access.get(itemStack);

                  String legacy = Text.LEGACY.serialize(
                      GlobalTranslator.render(name, Locale.ENGLISH)
                  );

                  Completions.suggest(builder1, legacy);
                });
              });
            })

            .executes(c -> {
              CommandSource source = c.getSource();

              ItemStack held = getHeld(source);
              ItemMeta meta = held.getItemMeta();
              Component name = Arguments.getMessage(c, "name").asComponent();

              if (Text.isDashClear(name)) {
                access.set(meta, null);
                source.sendSuccess(ItemMessages.NAME_CLEARED.renderText(source));
              } else {
                Component wrapped = Placeholders.render(optionallyWrap(name, c, "name"));
                access.set(meta, wrapped);

                source.sendSuccess(
                    ItemMessages.NAME_SET.get()
                        .addValue("name", wrapped)
                        .create(source)
                );
              }

              held.setItemMeta(meta);
              return 0;
            })
        );
  }

  public enum NameAccess {
    DISPLAY_NAME("name", "itemname", "nameitem", "renameitem", "itemrename") {
      @Override
      Component get(ItemStack item) {
        return Text.itemDisplayName(item);
      }

      @Override
      void set(ItemMeta meta, Component value) {
        meta.displayName(value);
      }
    },

    BASE_NAME ("base-name", "itembasename", "baseitemname") {
      @Override
      Component get(ItemStack item) {
        return item.getItemMeta().itemName();
      }

      @Override
      void set(ItemMeta meta, Component value) {
        meta.itemName(value);
      }
    };

    private final String argumentName;
    private final String commandName;
    private final String[] aliases;

    NameAccess(String argumentName, String commandName, String... aliases) {
      this.argumentName = argumentName;
      this.commandName = commandName;
      this.aliases = aliases;
    }

    abstract Component get(ItemStack item);

    abstract void set(ItemMeta meta, Component value);
  }
}