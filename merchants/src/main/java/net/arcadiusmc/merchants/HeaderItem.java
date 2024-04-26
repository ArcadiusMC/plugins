package net.arcadiusmc.merchants;

import static net.arcadiusmc.utils.io.ExtraCodecs.strictOptional;
import static net.kyori.adventure.text.Component.empty;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.arcadiusmc.menu.MenuNodeItem;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.inventory.SkullItemBuilder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

record HeaderItem(Component name, List<Component> lore, String textureId) implements MenuNodeItem {

  public static final HeaderItem DEFAULT = new HeaderItem(empty(), List.of(), "");

  static final Codec<HeaderItem> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            strictOptional(ExtraCodecs.COMPONENT, "name", empty())
                .forGetter(HeaderItem::name),

            strictOptional(ExtraCodecs.COMPONENT.listOf(), "lore", List.of())
                .forGetter(HeaderItem::lore),

            Codec.STRING.fieldOf("texture-id").forGetter(HeaderItem::textureId)
        )
        .apply(instance, HeaderItem::new);
  });

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    SkullItemBuilder builder = ItemStacks.headBuilder();

    if (!Text.isEmpty(name)) {
      builder.setName(name);
    }

    if (!lore.isEmpty()) {
      for (Component component : lore) {
        builder.addLore(component);
      }
    }

    if (!Strings.isNullOrEmpty(textureId)) {
      PlayerProfile profile = ItemStacks.profileFromTextureId(textureId);
      builder.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile));
    }

    return builder.build();
  }
}
