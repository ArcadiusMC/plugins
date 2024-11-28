package net.arcadiusmc.utils.inventory;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

@Getter
public class SkullItemBuilder extends ItemBuilder<SkullItemBuilder> {

  static final String BASE_URL = "http://textures.minecraft.net/texture/";

  public SkullItemBuilder(int amount) {
    super(Material.PLAYER_HEAD, amount);
  }

  public SkullItemBuilder(ItemStack stack, ItemMeta baseMeta) {
    super(stack, baseMeta);
  }

  private SkullMeta meta() {
    return (SkullMeta) baseMeta;
  }

  public SkullItemBuilder setTextureId(String textureId) {
    if (textureId == null) {
      return setProfile((PlayerProfile) null);
    }

    return setTextureUrl(BASE_URL + textureId);
  }

  public SkullItemBuilder setTextureUrl(String textureLink) {
    if (textureLink == null) {
      return setProfile((PlayerProfile) null);
    }

    SkullMeta meta = meta();
    PlayerProfile profile = meta.getPlayerProfile();

    if (profile == null) {
      profile = Bukkit.createProfile(UUID.randomUUID());
    }

    PlayerTextures textures = profile.getTextures();

    try {
      textures.setSkin(new URL(textureLink));
      profile.setTextures(textures);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    return setProfile(profile);
  }

  public SkullItemBuilder setProfile(PlayerProfile profile) {
    if (profile == null) {
      meta().setPlayerProfile(null);
      return this;
    }

    if (!profile.hasTextures()) {
      profile.complete(true);
    }

    meta().setPlayerProfile(profile);
    return this;
  }

  public SkullItemBuilder setProfile(OfflinePlayer profile) {
    return setProfile(profile.getPlayerProfile());
  }

  @Override
  protected SkullItemBuilder getThis() {
    return this;
  }
}