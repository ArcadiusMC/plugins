package net.arcadiusmc.punish;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.parse.ChatParseFlag;
import net.arcadiusmc.utils.Cooldown;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.command.CommandSender;
import org.slf4j.Logger;

/**
 * Checks and manages banned words in user messages and inputs.
 */
public final class BannedWords {

  private static final Logger LOGGER = Loggers.getLogger();

  private static final String FILENAME = "banned-words.toml";

  private static final String COOLDOWN_CATEGORY = "banned-words";
  private static final int COOLDOWN_TIME = 3 * 60 * 20;

  private final Path file;
  private BannedWordsConfig config = BannedWordsConfig.EMPTY;

  public BannedWords(Path file) {
    this.file = file.resolve(FILENAME);
  }

  public void load() {
    PluginJar.saveResources(FILENAME, file);

    SerializationHelper.readAsJson(file, json -> {
      BannedWordsConfig.loadConfig(json)
          .mapError(s -> "Failed to load " + file + " config: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(cfg -> config = cfg);
    });
  }

  public boolean contains(String unfiltered) {
    return contains(Text.renderString(unfiltered));
  }

  public boolean contains(ComponentLike component) {
    return containsBannedWords(Text.plain(component));
  }

  private boolean containsBannedWords(String input) {
    String filtered = config.filter(input);

    for (Pattern bannedWord : config.bannedWords()) {
      Matcher matcher = bannedWord.matcher(filtered);

      if (matcher.find()) {
        return true;
      }
    }

    return false;
  }

  public boolean checkAndWarn(CommandSender sender, ComponentLike component) {
    return _checkAndWarn(sender, Text.plain(component));
  }

  public boolean checkAndWarn(CommandSender sender, String input) {
    return checkAndWarn(sender, Text.renderString(sender, input));
  }

  private boolean _checkAndWarn(CommandSender sender, String input) {
    boolean senderHasBypass
        = sender == null || sender.hasPermission(ChatParseFlag.IGNORE_CASE.getPermission());

    if (sender == null || (senderHasBypass && config.allowBypass())) {
      return false;
    }

    boolean result = containsBannedWords(input);

    if (result && !Cooldown.containsOrAdd(sender, COOLDOWN_CATEGORY, COOLDOWN_TIME)) {
      sender.sendMessage(Messages.renderText("badLanguage.text", sender));
    }

    return result;
  }
}