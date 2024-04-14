package net.arcadiusmc.kingmaker;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.ListTag;
import net.forthecrown.nbt.TagTypes;
import org.slf4j.Logger;

public class Kingmaker {

  static final String TAG_MONARCH_ID = "monarch_id";
  static final String TAG_STANDS = "stands";

  private static final Logger LOGGER = Loggers.getLogger();

  private final List<MonarchStand> stands = new ArrayList<>();

  @Getter
  private UUID monarchId;

  private final KingmakerPlugin plugin;
  private final Path file;

  public Kingmaker(KingmakerPlugin plugin) {
    this.file = plugin.getDataFolder().toPath().resolve("data.dat");
    this.plugin = plugin;
  }

  public void setMonarch(User user) {
    Config config = plugin.getPluginConfig();
    String permissionGroup = config.monarchPermissionGroup();

    if (monarchId != null) {
      User existing = Users.get(monarchId);

      if (!Strings.isNullOrEmpty(permissionGroup)) {
        existing.unsetPermissionGroup(permissionGroup);
      }
    }

    if (user == null) {
      monarchId = null;

      for (MonarchStand stand : stands) {
        stand.update(null);
      }

      return;
    }

    monarchId = user.getUniqueId();

    if (!Strings.isNullOrEmpty(permissionGroup)) {
      user.setPermissionGroup(permissionGroup, true);
    }

    PlayerProfile profile = user.getProfile();

    if (!profile.hasTextures()) {
      profile.complete(true);
    }

    for (MonarchStand stand : stands) {
      stand.update(profile);
    }
  }

  public List<MonarchStand> getStands() {
    return Collections.unmodifiableList(stands);
  }

  public void addStand(MonarchStand stand) {
    Objects.requireNonNull(stand, "Null stand");
    stands.add(stand);

    if (monarchId == null) {
      stand.update(null);
      return;
    }

    User user = Users.get(monarchId);
    PlayerProfile profile = user.getProfile();

    if (!profile.hasTextures()) {
      profile.complete(true);
    }

    stand.update(profile);
  }

  public void clearStands() {
    stands.clear();
  }

  public void removeStand(int index) {
    stands.remove(index);
  }

  public void updateStands() {
    PlayerProfile profile;

    if (monarchId == null) {
      profile = null;
    } else {
      User user = Users.get(monarchId);
      profile = user.getProfile();

      if (!profile.hasTextures()) {
        profile.complete();
      }
    }

    for (MonarchStand stand : stands) {
      stand.update(profile);
    }
  }

  public void save() {
    SerializationHelper.writeTagFile(file, this::saveTo);
  }

  private void saveTo(CompoundTag tag) {
    if (monarchId != null) {
      tag.putUUID(TAG_MONARCH_ID, monarchId);
    }

    if (!stands.isEmpty()) {
      ListTag list = BinaryTags.listTag();

      for (MonarchStand stand : stands) {
        MonarchStand.CODEC.encodeStart(TagOps.OPS, stand)
            .mapError(s -> "Failed to save monarch stand: " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(list::add);
      }

      tag.put(TAG_STANDS, list);
    }
  }

  public void load() {
    stands.clear();
    monarchId = null;
    SerializationHelper.readTagFile(file, this::loadFrom);
  }

  private void loadFrom(CompoundTag tag) {
    if (tag.contains(TAG_MONARCH_ID)) {
      monarchId = tag.getUUID(TAG_MONARCH_ID);
    }

    if (tag.contains(TAG_STANDS, TagTypes.listType())) {
      ListTag list = tag.getList(TAG_STANDS);

      for (BinaryTag binaryTag : list) {
        MonarchStand.CODEC.parse(TagOps.OPS, binaryTag)
            .mapError(s -> "Failed to load monarch stand: " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(stands::add);
      }
    }
  }
}
