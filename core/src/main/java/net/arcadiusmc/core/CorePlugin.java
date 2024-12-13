package net.arcadiusmc.core;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
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

  private CoreConfig coreConfig;
  private PeriodicalSaver saver;

  private InventoryStorageImpl invStorage;
  private CooldownsImpl cooldowns;

  private UserServiceImpl userService;
  private HelpListImpl helpList;
  private ArcadiusServerImpl serverImpl;
  private PlaceholderServiceImpl placeholderService;
  private DayChange dayChange;
  private AutoAnnouncer announcer;
  private JoinInfo joinInfo;
  private Wild wild;
  private EmojiLoader emojiLoader;
  private TabMenu tabMenu;
  private CustomAdvancementRewards advancementRewards;

  public static CorePlugin plugin() {
    return getPlugin(CorePlugin.class);
  }

  @Override
  public void onEnable() {
    CoreDataFix.execute();
    Grenadier.plugin(this);
    GraveImpl.init();

    try {
      userService = new UserServiceImpl(this);
    } catch (Exception e) {
      PrintStream ps = new PrintStream(new FileOutputStream(FileDescriptor.err));

      ps.println(">> User system initialization error");

      e.printStackTrace(ps);

      ps.println(">>");
      ps.println(">> =======================================");
      ps.println(">> Failed to initialize user system...");
      ps.println(">> Initiating system halt");
      ps.println(">> Tell " + getPluginMeta().getAuthors());
      ps.println(">> =======================================");

      ps.flush();

      Runtime.getRuntime().halt(1);
    }

    helpList = new HelpListImpl();
    serverImpl = new ArcadiusServerImpl(this);
    dayChange = new DayChange();
    announcer = new AutoAnnouncer();
    placeholderService = new PlaceholderServiceImpl(this);
    joinInfo = new JoinInfo();
    wild = new Wild();
    emojiLoader = new EmojiLoader();
    tabMenu = new TabMenu(this);
    advancementRewards = new CustomAdvancementRewards(this);

    cooldowns = new CooldownsImpl(getDataPath());
    invStorage = new InventoryStorageImpl(getDataPath());

    BukkitServices.register(ArcadiusServer.class, serverImpl);
    BukkitServices.register(ArcadiusHelpList.class, helpList);
    BukkitServices.register(InventoryStorage.class, invStorage);
    BukkitServices.register(Cooldowns.class, cooldowns);
    BukkitServices.register(UserService.class, userService);
    BukkitServices.register(PlaceholderService.class, placeholderService);

    Users.setService(userService);
    userService.initialize();

    CoreListeners.registerAll(this);
    CoreCommands.createCommands(this);
    PrefsBook.init(serverImpl.getGlobalSettingsBook());

    saver = PeriodicalSaver.create(this::save, () -> coreConfig.autosaveInterval());

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
    coreConfig = TomlConfigs.loadPluginConfig(this, CoreConfig.class);
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
    if (userService != null) {
      userService.save();
    }
    if (serverImpl != null) {
      serverImpl.save();
    }

    if (invStorage != null) {
      invStorage.save();
    }
    if (cooldowns != null) {
      cooldowns.save();
    }
  }

  public void reloadAll() {
    userService.load();
    invStorage.load();
    cooldowns.load();

    reload();
  }

  public void reload() {
    reloadConfig();

    announcer.load();
    announcer.start();

    joinInfo.load();
    placeholderService.load();
    serverImpl.load();
    helpList.load();
    wild.load();
    emojiLoader.load();
    tabMenu.load();
    advancementRewards.load();
  }
}