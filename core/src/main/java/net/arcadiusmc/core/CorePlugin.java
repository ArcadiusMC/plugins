package net.arcadiusmc.core;

import lombok.Getter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.Cooldowns;
import net.arcadiusmc.InventoryStorage;
import net.arcadiusmc.command.help.ArcadiusHelpList;
import net.arcadiusmc.core.announcer.AutoAnnouncer;
import net.arcadiusmc.core.commands.CoreCommands;
import net.arcadiusmc.core.commands.help.HelpListImpl;
import net.arcadiusmc.core.grave.GraveImpl;
import net.arcadiusmc.core.listeners.CoreListeners;
import net.arcadiusmc.core.listeners.MobHealthBar;
import net.arcadiusmc.core.placeholder.PlaceholderServiceImpl;
import net.arcadiusmc.core.tab.TabMenu;
import net.arcadiusmc.core.user.UserServiceImpl;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.text.placeholder.PlaceholderService;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import net.forthecrown.grenadier.Grenadier;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class CorePlugin extends JavaPlugin {

  private CoreConfig ftcConfig;
  private PeriodicalSaver saver;

  private UserServiceImpl userService;
  private HelpListImpl helpList;
  private ArcadiusServerImpl ftcServer;
  private PlaceholderServiceImpl placeholderService;
  private DayChange dayChange;
  private AutoAnnouncer announcer;
  private JoinInfo joinInfo;
  private Wild wild;
  private EmojiLoader emojiLoader;
  private TabMenu tabMenu;

  public static CorePlugin plugin() {
    return getPlugin(CorePlugin.class);
  }

  @Override
  public void onEnable() {
    CoreDataFix.execute();
    Grenadier.plugin(this);
    GraveImpl.init();

    helpList = new HelpListImpl();
    userService = new UserServiceImpl(this);
    ftcServer = new ArcadiusServerImpl(this);
    dayChange = new DayChange();
    announcer = new AutoAnnouncer();
    placeholderService = new PlaceholderServiceImpl(this);
    joinInfo = new JoinInfo();
    wild = new Wild();
    emojiLoader = new EmojiLoader();
    tabMenu = new TabMenu(this);

    BukkitServices.register(ArcadiusServer.class, ftcServer);
    BukkitServices.register(ArcadiusHelpList.class, helpList);
    BukkitServices.register(InventoryStorage.class, InventoryStorageImpl.getStorage());
    BukkitServices.register(Cooldowns.class, CooldownsImpl.getCooldowns());
    BukkitServices.register(UserService.class, userService);
    BukkitServices.register(PlaceholderService.class, placeholderService);

    Users.setService(userService);
    userService.initialize();

    CoreListeners.registerAll(this);
    CoreCommands.createCommands(this);
    PrefsBook.init(ftcServer.getGlobalSettingsBook());

    saver = PeriodicalSaver.create(this::save, () -> ftcConfig.autosaveInterval());

    reloadAll();
  }

  @Override
  public void onLoad() {
    CoreFlags.registerAll();
  }

  @Override
  public void onDisable() {
    save();
    MobHealthBar.shutdown();
  }

  @Override
  public void reloadConfig() {
    ftcConfig = TomlConfigs.loadPluginConfig(this, CoreConfig.class);
    saver.start();
    dayChange.schedule();

    MessageLoader.loadPluginMessages(this, Messages.MESSAGE_LIST);
  }

  @Override
  public void saveConfig() {

  }

  @Override
  public void saveDefaultConfig() {
    saveResource("config.toml", false);
  }

  public void save() {
    userService.save();
    ftcServer.save();

    InventoryStorageImpl.getStorage().save();
    CooldownsImpl.getCooldowns().save();
  }

  public void reloadAll() {
    userService.load();
    InventoryStorageImpl.getStorage().load();
    CooldownsImpl.getCooldowns().load();

    reload();
  }

  public void reload() {
    reloadConfig();

    announcer.load();
    announcer.start();

    joinInfo.load();
    placeholderService.load();
    ftcServer.load();
    helpList.load();
    wild.load();
    emojiLoader.load();
    tabMenu.load();
  }
}