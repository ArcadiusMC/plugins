package net.arcadiusmc.markets;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.mail.Mail;
import net.arcadiusmc.markets.MarketsConfig.TaxBracket;
import net.arcadiusmc.signshops.SignShopFlags;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExistingObjectCodec.Builder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.slf4j.Logger;

public class Market {

  private static final Logger LOGGER = Loggers.getLogger();

  public static final int UNSET = -1;

  public static final Codec<Market> CODEC;

  @Getter
  private final String worldName;

  @Getter
  private final String regionName;

  @Getter @Setter
  private String groupName;

  private String mergedName;
  private final List<String> connected = new ArrayList<>();

  @Getter @Setter
  private UUID ownerId;

  @Getter
  private final Set<UUID> members = new HashSet<>();

  @Getter
  private final Set<UUID> bannedCustomers = new HashSet<>();

  @Getter
  private final List<Entrance> entrances = new ArrayList<>();

  private int price = UNSET;
  private int rent = UNSET;

  @Getter @Setter
  private int earnings = 0;

  @Getter @Setter
  private Instant lastRentTime;

  @Getter @Setter
  private Instant lastTaxReset;

  @Getter @Setter
  private Instant claimDate;

  @Getter
  MarketsManager manager;

  @Getter
  final ValueModifierList rentModifiers = new ValueModifierList();

  @Getter
  final ValueModifierList taxModifiers = new ValueModifierList();

  @Getter
  final ValueModifierList priceModifiers = new ValueModifierList();

  @Getter @Setter
  private int boundRenderOffset = 0;

  public Market(String worldName, String regionName) {
    Objects.requireNonNull(worldName, "Null world");
    Objects.requireNonNull(regionName, "Null region name");

    this.worldName = worldName;
    this.regionName = regionName;
  }

  public World getWorld() {
    return Bukkit.getWorld(worldName);
  }

  public Optional<ProtectedRegion> getRegion() {
    World worldValue = getWorld();

    if (worldValue == null) {
      return Optional.empty();
    }

    RegionManager manager = WorldGuard.getInstance()
        .getPlatform()
        .getRegionContainer()
        .get(BukkitAdapter.adapt(worldValue));

    if (manager == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(manager.getRegion(regionName));
  }

  public void syncRegion() {
    internalSyncRegion(true);
  }

  private void internalSyncRegion(boolean syncConnected) {
    if (manager == null) {
      LOGGER.warn("syncRegion called on an unregistered market: {}", regionName);
      return;
    }

    Optional<ProtectedRegion> opt = getRegion();

    if (opt.isEmpty()) {
      return;
    }

    ProtectedRegion region = opt.get();
    DefaultDomain domain = region.getMembers();
    domain.clear();

    forEachMember(domain::addPlayer);

    if (Strings.isNullOrEmpty(mergedName)) {
      region.setDirty(true);
      return;
    }

    Market merged = manager.getMarket(mergedName);

    if (merged == null) {
      region.setDirty(true);
      return;
    }

    merged.forEachMember(domain::addPlayer);
    region.setDirty(true);

    if (syncConnected) {
      merged.internalSyncRegion(false);
    }
  }

  void forEachMember(Consumer<UUID> idConsumer) {
    if (ownerId != null) {
      idConsumer.accept(ownerId);
    }

    members.forEach(idConsumer);
  }

  public boolean isPriceSet() {
    return price != UNSET;
  }

  public int getBasePrice() {
    if (price != UNSET || manager == null) {
      return price;
    }

    return manager.getPlugin().getPluginConfig().defaultPrice();
  }

  public int getPrice() {
    int base = getBasePrice();
    return priceModifiers.apply(base);
  }

  public boolean isRentSet() {
    return rent != UNSET;
  }

  public int getBaseRent() {
    if (rent != UNSET || manager == null) {
      return price;
    }
    return manager.getPlugin().getPluginConfig().baseRent();
  }

  public int getRent() {
    int base = getBaseRent();
    return rentModifiers.apply(base);
  }

  public void setPrice(int price) {
    if (price < 1) {
      this.price = UNSET;
      return;
    }

    this.price = price;
  }

  public void setRent(int rent) {
    if (rent < 1) {
      this.rent = UNSET;
      return;
    }

    this.rent = rent;
  }

  public void addEarnings(int earnings) {
    this.earnings += earnings;
  }

  public TaxBracket getTaxBracket() {
    if (manager == null) {
      return null;
    }

    MarketsConfig config = manager.getPlugin().getPluginConfig();
    for (TaxBracket taxBracket : config.taxBrackets()) {
      if (!taxBracket.earningsRange().contains(earnings)) {
        continue;
      }

      return taxBracket;
    }

    return null;
  }

  public float getBaseTaxRate() {
    TaxBracket bracket = getTaxBracket();
    if (bracket == null) {
      return 0.0f;
    }
    return bracket.rate();
  }

  public float getTaxRate() {
    return taxModifiers.apply(getBaseTaxRate());
  }

  public void connect(Market other) {
    other.connected.add(regionName);
    connected.add(other.regionName);
  }

  public void disconnect(Market other) {
    other.connected.remove(regionName);
    connected.remove(other.regionName);
  }

  public boolean isConnected(Market to) {
    return connected.contains(to.regionName);
  }

  public void clearConnected() {
    if (manager == null || connected.isEmpty()) {
      return;
    }

    for (String string : connected) {
      Market other = manager.getMarket(string);

      if (other == null) {
        continue;
      }

      other.connected.remove(regionName);
    }

    connected.clear();
  }

  public List<String> getConnectedNames() {
    return Collections.unmodifiableList(connected);
  }

  public boolean memberEditingAllowed() {
    return getRegion()
        .map(region -> region.getFlag(SignShopFlags.MEMBER_EDITING))
        .map(StateFlag::test)
        .orElse(false);
  }

  public boolean setMemberEditingAllowed(boolean state) {
    return getRegion()
        .map(region -> {
          region.setFlag(SignShopFlags.MEMBER_EDITING, state ? State.ALLOW : null);
          return true;
        })
        .orElse(false);
  }

  public Market getMerged() {
    if (manager == null || Strings.isNullOrEmpty(mergedName)) {
      return null;
    }

    return manager.getMarket(mergedName);
  }

  public boolean merge(Market other) {
    if (!Strings.isNullOrEmpty(mergedName) || !Strings.isNullOrEmpty(other.mergedName)) {
      return false;
    }

    mergedName = other.regionName;
    other.mergedName = regionName;

    syncRegion();
    return true;
  }

  public void unmerge() {
    if (Strings.isNullOrEmpty(mergedName) || manager == null) {
      return;
    }

    Market merged = manager.getMarket(mergedName);

    if (merged == null || !regionName.equals(merged.mergedName)) {
      return;
    }

    merged.mergedName = null;
    this.mergedName = null;

    merged.syncRegion();
    this.syncRegion();
  }

  public void claim(User user) {
    Preconditions.checkState(ownerId == null, "Shop already claimed");

    Instant now = Instant.now();

    setOwnerId(user.getUniqueId());
    setClaimDate(now);
    setLastRentTime(now);
    setLastTaxReset(now);
    syncRegion();

    manager.onClaim(this);

    MarketResets resets = manager.getPlugin().getResets();
    resets.onClaim(this);

    World world = getWorld();
    if (world == null) {
      return;
    }

    for (Entrance entrance : entrances) {
      entrance.onClaim(user, world);
    }
  }

  public void transfer(User user) {
    Preconditions.checkState(ownerId != null, "Shop not claimed");

    manager.onUnclaim(this);

    setOwnerId(user.getUniqueId());
    getMembers().clear();
    syncRegion();

    manager.onClaim(this);

    World world = getWorld();
    if (world == null) {
      return;
    }

    for (Entrance entrance : entrances) {
      entrance.onClaim(user, world);
    }
  }

  public void unclaim() {
    Preconditions.checkState(ownerId != null, "Shop not claimed");

    if (manager != null) {
      manager.onUnclaim(this);
    }

    if (!Strings.isNullOrEmpty(mergedName)) {
      unmerge();
    }

    setOwnerId(null);
    setClaimDate(null);
    getMembers().clear();
    setMemberEditingAllowed(false);
    syncRegion();

    World world = getWorld();
    if (world != null) {
      for (Entrance entrance : entrances) {
        entrance.onUnclaim(world, this);
      }
    }
  }

  private void rentTick(Instant now) {
    if (ownerId == null) {
      return;
    }

    if (lastRentTime == null) {
      lastRentTime = now;
    }

    MarketsConfig config = manager.getPlugin().getPluginConfig();
    Duration rentInterval = config.rentInterval();

    Instant nextExec = lastRentTime.plus(rentInterval);

    if (!now.isAfter(nextExec)) {
      return;
    }

    collectRent();
  }

  public void collectRent() {
    if (ownerId == null) {
      return;
    }

    User owner = Users.get(ownerId);
    int rent = getRent();

    lastRentTime = Instant.now();

    if (owner.hasBalance(rent)) {
      owner.removeBalance(rent);

      Component message = Messages.render("markets.rents.paid")
          .addValue("money", UnitFormat.currency(rent))
          .create(owner);

      Mail.sendOrMail(owner, message);
      return;
    }

    Debts debts = manager.getPlugin().getDebts();
    debts.getDebts().add(ownerId, rent);

    Component message = Messages.render("markets.rents.debt.added")
        .addValue("money", UnitFormat.currency(rent))
        .addValue("totalDebt", UnitFormat.currency(debts.getDebts().get(ownerId)))
        .create(owner);

    Mail.builder()
        .target(ownerId)
        .message(message)
        .send();
  }

  void tick(Instant now) {
    priceModifiers.tick(now);
    taxModifiers.tick(now);
    rentModifiers.tick(now);

    rentTick(now);
    taxesTick(now);
  }

  void taxesTick(Instant now) {
    if (lastTaxReset == null) {
      lastTaxReset = now;
      return;
    }

    MarketsConfig config = manager.getPlugin().getPluginConfig();
    Duration interval = config.taxResetInterval();

    Instant nextReset = lastTaxReset.plus(interval);

    if (!now.isAfter(nextReset)) {
      return;
    }

    earnings = 0;
    lastTaxReset = now;
  }

  void onAdded(MarketsManager manager) {
    this.manager = manager;
  }

  void onRemoved(MarketsManager manager) {
    this.manager = null;
  }

  public Component displayName() {
    TextWriter writer = TextWriters.newWriter();
    configureWriter(writer);
    writeInfo(writer);

    return Component.text("[" + regionName + "]")
        .hoverEvent(writer.asComponent());
  }

  public void configureWriter(TextWriter writer) {
    writer.setFieldStyle(Style.style(NamedTextColor.GRAY));
    writer.setFieldValueStyle(Style.style(NamedTextColor.YELLOW));
    writer.setFieldSeparator(Component.text(": ", NamedTextColor.GRAY));
  }

  public void writeInfo(TextWriter writer) {
    writer.field("World Name", worldName);
    writer.field("Region name", regionName);

    writer.newLine();
    writer.newLine();

    writer.field("Price", Text.formatNumber(getPrice()));
    writer.field("Rent", Text.formatNumber(getRent()));

    writer.newLine();
    writer.newLine();

    if (earnings > 0) {
      writer.field("Earnings", Text.formatNumber(earnings));
    }

    if (!Strings.isNullOrEmpty(groupName)) {
      writer.field("Group", groupName);
    }

    if (ownerId != null) {
      User user = Users.get(ownerId);
      writer.field("Owner", user.displayName());

      if (claimDate != null) {
        writer.field("Claim Date", Text.formatDate(claimDate));
      }
      if (lastRentTime != null) {
        writer.field("Last rent collection", Text.formatDate(lastRentTime));
      }

      float taxRate = getTaxRate();
      if (taxRate > 0) {
        writer.field("Tax Rate", Text.formatNumber(taxRate));
      }
    }

    if (!members.isEmpty()) {
      writer.field("Members:");

      members.stream()
          .map(Users::get)
          .map(User::displayName)
          .forEach(component -> {
            writer.line("- ", writer.getFieldStyle());
            writer.write(component);
          });
    }


  }

  static {
    Codec<Market> initialCodec = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.STRING.fieldOf("world").forGetter(o -> o.worldName),
              Codec.STRING.fieldOf("region").forGetter(o -> o.regionName)
          )
          .apply(instance, Market::new);
    });

    Builder<Market> builder = ExistingObjectCodec.builder();

    builder.optional("connected", Codec.STRING.listOf())
        .getter(market -> market.connected)
        .setter((market, stringList) -> market.connected.addAll(stringList))
        .excludeIf(List::isEmpty);

    builder.optional("merged", Codec.STRING)
        .getter(market -> market.mergedName)
        .setter((market, s) -> market.mergedName = s)
        .excludeIf(Strings::isNullOrEmpty);

    builder.optional("owner_id", ExtraCodecs.UUID_CODEC)
        .getter(market -> market.ownerId)
        .setter((market, uuid) -> market.ownerId = uuid);

    builder.optional("claim_date", ExtraCodecs.INSTANT)
        .getter(market -> market.claimDate)
        .setter((market, instant) -> market.claimDate = instant);

    builder.optional("last_tax_reset", ExtraCodecs.INSTANT)
        .getter(market -> market.lastTaxReset)
        .setter((market, instant) -> market.lastTaxReset = instant);

    builder.optional("last_rent_collected", ExtraCodecs.INSTANT)
        .getter(market -> market.lastRentTime)
        .setter((market, instant) -> market.lastRentTime = instant);

    builder.optional("member_ids", ExtraCodecs.UUID_CODEC.listOf())
        .getter(market -> new ArrayList<>(market.members))
        .setter((market, uuids) -> market.members.addAll(uuids))
        .excludeIf(List::isEmpty);

    builder.optional("banned_customer_ids", ExtraCodecs.UUID_CODEC.listOf())
        .getter(market -> new ArrayList<>(market.bannedCustomers))
        .setter((market, uuids) -> market.bannedCustomers.addAll(uuids))
        .excludeIf(List::isEmpty);

    builder.optional("price", Codec.INT)
        .getter(market -> market.price)
        .setter(Market::setPrice)
        .excludeIf(integer -> integer == UNSET);

    builder.optional("rent", Codec.INT)
        .getter(market -> market.rent)
        .setter(Market::setRent)
        .excludeIf(integer -> integer == UNSET);

    builder.optional("group", ExtraCodecs.KEY_CODEC)
        .getter(market -> market.groupName)
        .setter((market, s) -> market.groupName = s)
        .excludeIf(Strings::isNullOrEmpty);

    builder.optional("entrances", Entrance.CODEC.listOf())
        .getter(market -> market.entrances)
        .setter((market, entrances1) -> market.entrances.addAll(entrances1))
        .excludeIf(List::isEmpty);

    builder.optional("rent_modifiers", ValueModifierList.CODEC)
        .getter(market -> market.rentModifiers)
        .setter((market, list) -> market.rentModifiers.addAll(list))
        .excludeIf(ValueModifierList::isEmpty);

    builder.optional("price_modifiers", ValueModifierList.CODEC)
        .getter(market -> market.priceModifiers)
        .setter((market, list) -> market.priceModifiers.addAll(list))
        .excludeIf(ValueModifierList::isEmpty);

    builder.optional("tax_modifiers", ValueModifierList.CODEC)
        .getter(market -> market.taxModifiers)
        .setter((market, list) -> market.taxModifiers.addAll(list))
        .excludeIf(ValueModifierList::isEmpty);

    builder.optional("bound_render_offset", Codec.INT)
        .getter(market -> market.boundRenderOffset)
        .setter((market, integer) -> market.boundRenderOffset = integer)
        .excludeIf(integer -> integer == 0);

    CODEC = builder.build().codec(initialCodec);
  }
}
