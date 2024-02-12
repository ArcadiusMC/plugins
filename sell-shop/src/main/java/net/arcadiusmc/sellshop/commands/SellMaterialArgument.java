package net.arcadiusmc.sellshop.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.sellshop.data.ItemDataMap;
import net.arcadiusmc.sellshop.data.ItemSellData;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.Material;
import org.bukkit.Registry;

public class SellMaterialArgument implements ArgumentType<Material> {

  private final ItemDataMap map;
  private final ArgumentType<Material> matParser;

  public SellMaterialArgument(ItemDataMap map) {
    this.map = map;
    this.matParser = ArgumentTypes.registry(Registry.MATERIAL, "Material");
  }

  @Override
  public Material parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();

    Material material = matParser.parse(reader);
    ItemSellData data = map.getData(material);

    if (data == null) {
      reader.setCursor(start);
      
      throw Messages.render("sellshop.errors.notSellable")
          .addValue("material", material)
          .exception();
    }

    return material;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    return Completions.suggest(builder, map::keyIterator);
  }
}
