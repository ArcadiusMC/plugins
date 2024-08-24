package net.arcadiusmc.core.user;

import com.google.common.base.Preconditions;
import com.google.common.reflect.Reflection;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.core.CoreConfig;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.user.PropertyImpl.BuilderImpl;
import net.arcadiusmc.holograms.Holograms;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserComponent;
import net.arcadiusmc.user.UserLookup;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.user.UserProperty.Builder;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.user.event.UserLeaveEvent;
import net.arcadiusmc.user.event.UserLogEvent;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.ScoreIntMap;
import net.arcadiusmc.utils.Time;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PathUtil;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Internal
@Getter
public class UserServiceImpl implements UserService {

  public static final Logger LOGGER = Loggers.getLogger();

  private final CorePlugin plugin;

  private final UserDataStorage storage;

  private final UserLookupImpl lookup;
  private final NameFactoryImpl nameFactory;
  private final UserMaps userMaps;
  private final AltUsers altUsers;
  private final Flags flags;

  private final ScoreIntMap<UUID> balances;
  private final ScoreIntMap<UUID> gems;
  private final ScoreIntMap<UUID> playtime;
  private final ScoreIntMap<UUID> monthlyPlaytime;
  private final ScoreIntMap<UUID> votes;

  private final Registry<UserProperty<?>> propertyRegistry;

  private final Registry<Currency> currencies;

  private boolean componentRegistryFrozen = false;

  private final List<String> defunctProperties = new ArrayList<>();

  public UserServiceImpl(CorePlugin plugin) {
    this.plugin = plugin;

    Path dir = PathUtil.pluginPath();
    this.storage          = new UserDataStorage(dir);

    this.lookup           = new UserLookupImpl();
    this.nameFactory      = new NameFactoryImpl();
    this.userMaps         = new UserMaps(this);
    this.altUsers         = new AltUsers();
    this.flags            = new Flags();

    this.balances         = new ScoreIntMap<>();
    this.gems             = new ScoreIntMap<>();
    this.playtime         = new ScoreIntMap<>();
    this.votes            = new ScoreIntMap<>();
    this.monthlyPlaytime  = new ScoreIntMap<>();

    this.currencies       = Registries.newFreezable();

    this.propertyRegistry = Registries.newFreezable();
    this.propertyRegistry.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<UserProperty<?>> value) {
        PropertyImpl<?> property = (PropertyImpl<?>) value.getValue();
        property.id = value.getId();
        property.key = value.getKey();
      }

      @Override
      public void onUnregister(Holder<UserProperty<?>> value) {
        PropertyImpl<?> property = (PropertyImpl<?>) value.getValue();
        property.id = -1;
        property.key = null;
      }
    });

  }

  public CoreConfig getConfig() {
    return plugin.getCoreConfig();
  }

  @Override
  public boolean userLoadingAllowed() {
    return userMaps.isLoadingEnabled();
  }

  public void initialize() {
    NameElements.registerAll(nameFactory);
    ProfileFields.registerAll(nameFactory);

    // *Spongebob stinky sound effect*
    Reflection.initialize(Properties.class);

    registerComponent(PropertyMap.class);
    registerComponent(BlockListImpl.class);
    registerComponent(UserTimestamps.class);
    registerComponent(UserHomes.class);

    CurrencyImpl balances = new CurrencyImpl(this.balances,
        Messages.MESSAGE_LIST.reference("units.currency.singular"),
        Messages.MESSAGE_LIST.reference("units.currency.plural"),
        Messages.MESSAGE_LIST.reference("units.display")
    );

    CurrencyImpl gems = new CurrencyImpl(this.gems,
        Messages.MESSAGE_LIST.reference("units.gems.singular"),
        Messages.MESSAGE_LIST.reference("units.gems.plural"),
        Messages.MESSAGE_LIST.reference("units.display")
    );

    currencies.register("balances", balances);
    currencies.register("gems", gems);
  }

  public void shutdown() {
    save();

    userMaps.getOnline().forEach(user -> {
      onUserLeave(user, QuitReason.DISCONNECTED, false);
    });
    userMaps.setLoadingEnabled(false);
  }

  public void save() {
    storage.saveMap(balances, storage.getBalances());
    storage.saveMap(gems,     storage.getGems());
    storage.saveMap(playtime, storage.getPlaytime());
    storage.saveMap(votes,    storage.getVotes());

    storage.saveMap(monthlyPlaytime, storage.getMonthlyPlaytime());

    storage.saveProfiles(lookup);
    storage.saveAlts(altUsers);
    storage.saveFlags(flags);

    var userIt = userMaps.iterator();
    while (userIt.hasNext()) {
      var user = userIt.next();
      storage.saveUser(user);

      if (!user.isOnline() && userMaps.isUserAutoUnloadingEnabled()) {
        userIt.remove();
      }
    }
  }

  public void load() {
    storage.loadProfiles(lookup);
    storage.loadAlts(altUsers);
    storage.loadFlags(flags);

    storage.loadMap(balances, storage.getBalances());
    storage.loadMap(gems,     storage.getGems());
    storage.loadMap(playtime, storage.getPlaytime());
    storage.loadMap(votes,    storage.getVotes());

    storage.loadMap(monthlyPlaytime, storage.getMonthlyPlaytime());

    for (UserImpl user : userMaps) {
      storage.loadUser(user);
    }
  }

  public void unloadUser(UserImpl user) {
    userMaps.remove(user);
  }

  public void onUserLeave(UserImpl user, QuitReason reason, boolean announce) {
    Player player = user.getPlayer();

    UserLeaveEvent userEvent = new UserLeaveEvent(user, reason, false);
    userEvent.callEvent();

    if (announce) {
      UserLogEvent.maybeAnnounce(userEvent);
    }

    if (player != null) {
      user.setEntityLocation(player.getLocation());
      user.setLastOnlineName(player.getName());

      var addr = player.getAddress();
      if (addr != null) {
        var entry = user.lookupEntry();
        lookup.changeIp(entry, addr.getAddress().getHostAddress());
      }
    }

    if (user.vanishTicker != null) {
      user.vanishTicker.stop();
      user.vanishTicker = null;
    }

    // Log play time
    logTime(user).apply(LOGGER::error, seconds -> {
      long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();

      // Prevent excessive playtime bug
      if (TimeUnit.SECONDS.toMillis(seconds) > uptimeMillis) {
        LOGGER.warn(
            "Playtime bug in user {}, online for {} seconds ({} hours)???",
            user, seconds, TimeUnit.SECONDS.toHours(seconds)
        );

        return;
      } else {
        LOGGER.info(
            "Adding {} seconds or {} hours to {}'s playtime",
            seconds, TimeUnit.SECONDS.toHours(seconds), user
        );
      }

      playtime.add(user.getUniqueId(), seconds);
      monthlyPlaytime.add(user.getUniqueId(), seconds);

      Holograms.updateWithSource("playtime/total");
      Holograms.updateWithSource("playtime/monthly");
    });

    LOGGER.debug("calling unloadUser on onUserLeave");
    unloadUser(user);
  }

  /**
   * Logs the player's playtime
   */
  private Result<Integer> logTime(UserImpl user) {
    long join = user.getTime(TimeField.LAST_LOGIN);

    if (join == -1) {
      return Result.error("Join time not set");
    }

    long played = Time.timeSince(join) - user.getTime(TimeField.AFK_TIME);
    int timeSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(played);

    return Result.success(timeSeconds);
  }

  public void onServerLoaded() {
    propertyRegistry.freeze();
    componentRegistryFrozen = true;
    userMaps.setLoadingEnabled(true);
    TimeField.freeze();
  }

  @Override
  public CompletableFuture<Collection<User>> loadAllUsers() {
    return CompletableFuture.supplyAsync(() -> {
      List<User> users = new ObjectArrayList<>();
      executeOnAllUserInternal(users::add, false);
      return users;
    });
  }

  @Override
  public void executeOnAllUsers(Consumer<User> operation) {
    executeOnAllUserInternal(operation, true);
  }

  @Override
  public void executeOnAllUsersAsync(Consumer<User> operation) {
    CompletableFuture.runAsync(() -> executeOnAllUserInternal(operation, true));
  }

  private void executeOnAllUserInternal(Consumer<User> operation, boolean unload) {
    if (!unload) {
      userMaps.setUserAutoUnloadingEnabled(false);
    }

    lookup.stream().forEach(entry -> {
      UserImpl user = userMaps.getUser(entry);
      operation.accept(user);

      if (unload) {
        unloadUser(user);
      }
    });

    userMaps.setUserAutoUnloadingEnabled(true);
  }

  @Override
  public Registry<Currency> getCurrencies() {
    return currencies;
  }

  @Override
  public Registry<UserProperty<?>> getUserProperties() {
    return propertyRegistry;
  }

  @Override
  public void setPropertyDefunct(String propertyId) {
    Objects.requireNonNull(propertyId, "Null propertyId");
    Preconditions.checkArgument(
        !propertyRegistry.contains(propertyId),
        "propertyId belongs to currently registered property"
    );

    defunctProperties.add(propertyId);
  }

  void ensureNotFrozen() {
    if (!propertyRegistry.isFrozen()) {
      return;
    }

    throw new IllegalStateException(
        "UserProperty registry is frozen and cannot be modified"
    );
  }

  @Override
  public Builder<UUID> createUuidProperty() {
    ensureNotFrozen();
    return new BuilderImpl<>(ExtraCodecs.STRING_UUID, ArgumentTypes.uuid())
        .defaultValue(Identity.nil().uuid());
  }

  @Override
  public Builder<Boolean> createBooleanProperty() {
    ensureNotFrozen();
    return new BuilderImpl<>(Codec.BOOL, BoolArgumentType.bool());
  }

  @Override
  public Builder<Component> createTextProperty() {
    ensureNotFrozen();
    return new BuilderImpl<>(
        ExtraCodecs.COMPONENT,
        ArgumentTypes.map(Arguments.CHAT, ViewerAwareMessage::asComponent)
    );
  }

  @Override
  public Builder<String> createStringProperty() {
    ensureNotFrozen();
    return new BuilderImpl<>(Codec.STRING, StringArgumentType.greedyString());
  }

  @Override
  public <E extends Enum<E>> Builder<E> createEnumProperty(Class<E> type) {
    ensureNotFrozen();
    return new BuilderImpl<>(ExtraCodecs.enumCodec(type), ArgumentTypes.enumType(type));
  }

  @Override
  public Collection<User> getOnlineUsers() {
    return Collections.unmodifiableCollection(userMaps.getOnline());
  }

  @Override
  public void registerComponent(Class<? extends UserComponent> componentType) {
    Preconditions.checkState(
        !componentRegistryFrozen,
        "Component registry is frozen and cannot be modified"
    );

    Components.createFactory(componentType);
  }

  @Override
  public @NotNull UserImpl getUser(@NotNull UserLookup.LookupEntry entry) {
    return userMaps.getUser(entry);
  }

  @Override
  public @Nullable UUID getMainAccount(@NotNull UUID altPlayerId) {
    Objects.requireNonNull(altPlayerId);
    return altUsers.getMain(altPlayerId);
  }

  @Override
  public boolean isAltAccount(@NotNull UUID playerId) {
    Objects.requireNonNull(playerId);
    return altUsers.isAlt(playerId);
  }

  @Override
  public Collection<UUID> getOtherAccounts(@NotNull UUID playerId) {
    Objects.requireNonNull(playerId);
    return altUsers.getOtherAccounts(playerId);
  }

  @Override
  public Collection<UUID> getAltAccounts(@NotNull UUID playerId) {
    Objects.requireNonNull(playerId);
    return altUsers.getAlts(playerId);
  }

  @Override
  public boolean isAltForAny(@NotNull UUID playerId, @NotNull Collection<Player> players) {
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(players);
    return altUsers.isAltForAny(playerId, players);
  }

  @Override
  public void setAltAccount(@NotNull UUID altPlayerId, @NotNull UUID mainPlayerId) {
    Objects.requireNonNull(altPlayerId);
    Objects.requireNonNull(mainPlayerId);
    altUsers.addEntry(altPlayerId, mainPlayerId);
  }

  @Override
  public void removeAltAccount(@NotNull UUID altPlayerId) {
    Objects.requireNonNull(altPlayerId);
    altUsers.removeEntry(altPlayerId);
  }
}