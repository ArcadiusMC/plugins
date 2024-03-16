package net.arcadiusmc.factions;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.ArrayIterator;
import net.arcadiusmc.utils.io.TagOps;
import net.arcadiusmc.utils.property.IdPropertyMap;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class Faction {

  private static final Logger LOGGER = Loggers.getLogger();

  static final String TAG_MEMBERS = "members";
  static final String TAG_PROPERTIES = "properties";

  @Getter
  private final String key;

  private final Map<UUID, FactionMember> memberMap = new HashMap<>();

  private final IdPropertyMap properties = new IdPropertyMap();
  private CompoundTag unknownProperties;

  public Faction(String key) {
    Objects.requireNonNull(key, "Null key");
    this.key = key;
  }

  public Stream<FactionMember> getActiveMembers() {
    return memberMap.values().stream().filter(FactionMember::isActive);
  }

  public FactionMember getMember(UUID playerId) {
    return memberMap.get(playerId);
  }

  public FactionMember getActiveMember(UUID playerId) {
    FactionMember member = getMember(playerId);

    if (member == null || !member.isActive()) {
      return null;
    }

    return member;
  }

  public Component displayName(Audience viewer) {
    Component baseName;
    Component displayName = get(Properties.DISPLAY_NAME);

    if (displayName == null) {
      baseName = Component.text(key);
    } else {
      baseName = displayName;
    }

    Component message = Messages.render("factions.names.format")
        .addValue("base", baseName)
        .create(viewer);

    return message.color(get(Properties.NAME_COLOR));
  }

  public void join(User user) {
    FactionMember member = memberMap.computeIfAbsent(user.getUniqueId(), FactionMember::new);
    member.setActive(true);

    if (user.isOnline()) {
      user.sendMessage(
          Messages.render("factions.joined")
              .addValue("faction", displayName(user))
              .create(user)
      );
    }

    String lpGroup = get(Properties.LP_GROUP);
    if (!Strings.isNullOrEmpty(lpGroup)) {
      Commands.executeConsole("lp user %s parent add %s", user.getName(), lpGroup);
    }

    if (Factions.isDiscordEnabled()) {
      FactionsDiscord.onJoin(this, user);
    }
  }

  public void leave(User user) {
    FactionMember member = getActiveMember(user.getUniqueId());

    if (member == null) {
      return;
    }

    member.setActive(false);

    FactionsConfig config = Factions.getConfig();
    int repLoss = config.getReputationPenalty();
    int newBaseReputation = member.getBaseReputation() - repLoss;

    member.setBaseReputation(newBaseReputation);

    user.sendMessage(
        Messages.render("factions.left")
            .addValue("faction", displayName(user))
            .addValue("reputationDrop", repLoss)
            .create(user)
    );

    String lpGroup = get(Properties.LP_GROUP);
    if (!Strings.isNullOrEmpty(lpGroup)) {
      Commands.executeConsole("lp user %s parent remove %s", user.getName(), lpGroup);
    }

    if (Factions.isDiscordEnabled()) {
      FactionsDiscord.onLeave(this, user);
    }
  }

  /* --------------------------- properties ---------------------------- */

  public <T> boolean has(@NotNull FactionProperty<T> property) {
    return properties.has(property);
  }

  public <T> T get(@NotNull FactionProperty<T> property) {
    return properties.get(property);
  }

  public <T> boolean set(@NotNull FactionProperty<T> property, @Nullable T value) {
    T current = get(property);
    boolean wasChanged = properties.set(property, value);

    if (wasChanged) {
      property.onUpdate(this, current, value);
    }

    return wasChanged;
  }

  /* --------------------------- serialization ---------------------------- */

  public void save(CompoundTag tag) {
    if (!memberMap.isEmpty()) {
      FactionMember.LIST_CODEC.encodeStart(TagOps.OPS, new ArrayList<>(memberMap.values()))
          .mapError(s -> key + ": Failed to save faction member list: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(binaryTag -> tag.put(TAG_MEMBERS, binaryTag));
    }

    if (!properties.isEmpty()) {
      ArrayIterator<Object> it = properties.iterator();
      CompoundTag propertyTag = BinaryTags.compoundTag();

      if (unknownProperties != null && !unknownProperties.isEmpty()) {
        tag.putAll(unknownProperties);
      }

      while (it.hasNext()) {
        int id = it.nextIndex();
        Object n = it.next();

        Properties.REGISTRY.getHolder(id).ifPresentOrElse(
            holder -> {
              Codec<Object> codec = (Codec) holder.getValue().getCodec();

              codec.encodeStart(TagOps.OPS, n)
                  .mapError(s -> key + ": Failed to save property " + holder.getKey() + ": " + s)
                  .resultOrPartial(LOGGER::error)
                  .ifPresent(binaryTag -> propertyTag.put(holder.getKey(), binaryTag));
            },
            () -> {
              LOGGER.error("Unknown property in faction {}, id={}", key, id);
            }
        );
      }

      tag.put(TAG_PROPERTIES, propertyTag);
    }
  }

  public void load(CompoundTag tag) {
    if (tag.containsKey(TAG_MEMBERS)) {
      FactionMember.LIST_CODEC.parse(TagOps.OPS, tag.get(TAG_MEMBERS))
          .mapError(s -> key + ": Failed to load members list: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(factionMembers -> {
            for (FactionMember member : factionMembers) {
              memberMap.put(member.getPlayerId(), member);
            }
          });
    }

    if (tag.containsKey(TAG_PROPERTIES)) {
      CompoundTag propertyTag = tag.getCompound(TAG_PROPERTIES);

      for (Entry<String, BinaryTag> entry : propertyTag.entrySet()) {
        String key = entry.getKey();
        Optional<FactionProperty<?>> opt = Properties.REGISTRY.get(key);

        if (opt.isEmpty()) {
          LOGGER.warn("{}: Unknown faction property '{}'", this.key, key);

          if (unknownProperties == null) {
            unknownProperties = BinaryTags.compoundTag();
            unknownProperties.put(key, entry.getValue());
          }

          continue;
        }

        FactionProperty<Object> property = (FactionProperty<Object>) opt.get();
        Codec<Object> codec = property.getCodec();

        codec.parse(TagOps.OPS, entry.getValue())
            .mapError(s -> this.key + ": Failed to load property " + key + ": " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(o -> properties.set(property, o));
      }
    }
  }
}
