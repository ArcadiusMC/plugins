package net.arcadiusmc.core.commands.item;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.chat.MessageSuggestions;
import net.arcadiusmc.command.help.Usage;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.commands.CommandNickname;
import net.arcadiusmc.text.Text;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameNode extends ItemModifierNode {

  public ItemNameNode() {
    super("itemname", "nameitem", "renameitem", "itemrename");
  }

  @Override
  String getArgumentName() {
    return "name";
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    namingNote(
        factory.usage("<text>")
            .addInfo("Sets the name of the item you're holding")
    );

    factory.usage("-clear")
        .addInfo("Clears the name of the item you're holding");
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
                  var name = Text.itemDisplayName(itemStack);

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
                meta.displayName(null);
                source.sendSuccess(ItemMessages.NAME_CLEARED.renderText(source));
              } else {
                meta.displayName(optionallyWrap(name, c, "name"));
                source.sendSuccess(
                    ItemMessages.NAME_SET.get()
                        .addValue("name", name)
                        .create(source)
                );
              }

              held.setItemMeta(meta);
              return 0;
            })
        );
  }
}