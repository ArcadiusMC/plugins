package net.arcadiusmc.core.commands.help;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.help.ArcadiusHelpList;
import net.arcadiusmc.command.help.CommandHelpEntry;
import net.arcadiusmc.command.help.HelpEntry;
import net.arcadiusmc.command.help.Usage;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Grenadier;
import org.slf4j.Logger;

public class HelpListImpl implements ArcadiusHelpList {

  public static final Logger LOGGER = Loggers.getLogger();

  private static final Comparator<ObjectIntPair<HelpEntry>> COMPARATOR;

  private static final Comparator<HelpEntry> ALPHABETIC_COMPARATOR
      = Comparator.comparing(HelpEntry::getMainLabel);

  /** Map of all existing commands, mapped to their name */
  private final Map<String, BaseCommand> existingCommands = new HashMap<>();

  private final List<HelpEntry> entries = new ObjectArrayList<>();
  private final List<LoadedHelpEntry> loadedEntries = new ObjectArrayList<>();

  private final Path topicsFile;
  private final Path usagesFile;

  static {
    Comparator<ObjectIntPair<HelpEntry>> cmp = Comparator.comparingInt(ObjectIntPair::rightInt);
    COMPARATOR = cmp.thenComparing(pair -> pair.left().getMainLabel());
  }

  public HelpListImpl() {
    this.topicsFile = PathUtil.pluginPath("help_topics.yml");
    this.usagesFile = PathUtil.pluginPath("extra-command-help.yml");
  }

  /** Suggests help entry keywords */
  @Override
  public CompletableFuture<Suggestions> suggest(CommandSource source, SuggestionsBuilder builder) {
    var input = builder.getRemainingLowerCase();
    boolean beginsWithQuote
        = input.length() > 0
        && StringReader.isQuotedStringStart(input.charAt(0));

    char quote;

    if (beginsWithQuote) {
      quote = input.charAt(0);
    } else {
      quote = '"';
    }

    String unquoted = input.replaceAll(quote + "", "");

    getAll().stream()
        .filter(entry -> entry.test(source))
        .flatMap(entry -> entry.getKeywords().stream())
        .filter(s -> Completions.matches(unquoted, s))
        .map(s -> Commands.optionallyQuote(quote + "", s))
        .forEach(builder::suggest);

    return builder.buildFuture();
  }

  @Override
  public List<HelpEntry> query(CommandSource source, String tag) {
    List<HelpEntry> entries = new ObjectArrayList<>();

    if (Strings.isNullOrEmpty(tag) || tag.equalsIgnoreCase("all")) {
      entries.addAll(getAll());

      // Remove the ones the source doesn't have permission to see
      entries.removeIf(entry -> !entry.test(source));
      entries.sort(ALPHABETIC_COMPARATOR);
    } else {
      List<ObjectIntPair<HelpEntry>> lookupResult = lookup(normalize(tag), source);
      lookupResult.sort(COMPARATOR);
      lookupResult.stream().map(Pair::left).forEach(entries::add);
    }

    return entries;
  }

  private List<ObjectIntPair<HelpEntry>> lookup(String tag, CommandSource source) {
    // Keyword lookup failed, loop through all keywords to find the ones
    // that match the most
    List<ObjectIntPair<HelpEntry>> result = new ObjectArrayList<>();
    final int maxDistance = 3;

    for (var v: getAll()) {
      if (!v.test(source)) {
        continue;
      }

      var keywords = v.getKeywords();

      for (var keyword: keywords) {
        var s = normalize(keyword);
        int dis = levenshteinDistance(tag, s);

        // -1 means above max threshold
        if (dis == -1 || dis > maxDistance) {
          continue;
        }

        if (dis == 0) {
          result = new ObjectArrayList<>();
          result.add(ObjectIntPair.of(v, dis));
          return result;
        }

        result.add(ObjectIntPair.of(v, dis));
        break;
      }
    }

    return result;
  }

  // Copy-pasted from org.bukkit.command.defaults.HelpCommand
  private static int levenshteinDistance(String s1, String s2) {
    if (s1 == null && s2 == null) {
      return 0;
    }
    if (s1 != null && s2 == null) {
      return s1.length();
    }
    if (s1 == null && s2 != null) {
      return s2.length();
    }

    int s1Len = s1.length();
    int s2Len = s2.length();
    int[][] H = new int[s1Len + 2][s2Len + 2];

    int INF = s1Len + s2Len;
    H[0][0] = INF;
    for (int i = 0; i <= s1Len; i++) {
      H[i + 1][1] = i;
      H[i + 1][0] = INF;
    }
    for (int j = 0; j <= s2Len; j++) {
      H[1][j + 1] = j;
      H[0][j + 1] = INF;
    }

    Char2IntMap sd = new Char2IntOpenHashMap();
    sd.defaultReturnValue(0);

    for (int i = 1; i <= s1Len; i++) {
      int DB = 0;
      for (int j = 1; j <= s2Len; j++) {
        int i1 = sd.get(s2.charAt(j - 1));
        int j1 = DB;

        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
          H[i + 1][j + 1] = H[i][j];
          DB = j;
        } else {
          H[i + 1][j + 1] = Math.min(H[i][j], Math.min(H[i + 1][j], H[i][j + 1])) + 1;
        }

        H[i + 1][j + 1] = Math.min(H[i + 1][j + 1], H[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1));
      }
      sd.put(s1.charAt(i - 1), i);
    }

    return H[s1Len + 1][s2Len + 1];
  }

  private Collection<HelpEntry> getAll() {
    return entries;
  }

  public Collection<BaseCommand> getExistingCommands() {
    return Collections.unmodifiableCollection(existingCommands.values());
  }

  public Collection<HelpEntry> getAllEntries() {
    return Collections.unmodifiableCollection(entries);
  }

  @Override
  public Collection<HelpEntry> getEntries(String keyword) {
    return entries.stream()
        .filter(entry -> entry.getKeywords().contains(keyword))
        .toList();
  }

  public void addCommand(BaseCommand command) {
    existingCommands.put(command.getName(), command);
  }

  /**
   * When commands are created, their aliases, permissions and so forth, get
   * set after the {@link BaseCommand} constructor is called, so this method MUST
   * be called after ALL commands have been created and registered for it to
   * properly index every keyword, alias and command label.
   */
  public void update() {
    entries.removeIf(entry -> entry instanceof CommandHelpEntry);

    existingCommands.forEach((s, command) -> {
      if (command instanceof LoadedEntryCommand) {
        return;
      }

      CommandHelpEntry entry = new CommandHelpEntry(command);

      UsageFactory factory = arguments -> {
        Usage usage = new Usage(arguments);
        entry.getUsages().add(usage);
        return usage;
      };

      if (command.isSimpleUsages()) {
        factory.usage("").addInfo(command.getDescription());
      }

      command.populateUsages(factory);
      addEntry(entry);
    });
  }

  @Override
  public void addEntry(HelpEntry entry) {
    entries.add(entry);
  }

  private static String normalize(String s) {
    if (s.startsWith("/")) {
      s = s.substring(1);
    }

    return s.toLowerCase()
        .trim()
        .replaceAll(" ", "_");
  }

  private void clearDynamicallyLoaded() {
    entries.removeIf(entry -> {
      return entry instanceof LoadedHelpEntry
          || entry instanceof LoadedCommandEntry;
    });

    existingCommands.values().removeIf(cmd -> cmd instanceof LoadedEntryCommand);

    CommandDispatcher<CommandSource> dispatcher = Grenadier.dispatcher();

    for (LoadedHelpEntry entry : loadedEntries) {
      if (entry.getCommand() == null) {
        continue;
      }

      var cmd = entry.getCommand();
      Commands.removeChild(dispatcher.getRoot(), cmd.getName());
    }

    loadedEntries.clear();
  }

  public void load() {
    clearDynamicallyLoaded();

    PluginJar.saveResources("help_topics.yml");
    PluginJar.saveResources("extra-command-help.yml");

    SerializationHelper.readAsJson(topicsFile, this::loadHelpEntriesFrom);

    SerializationHelper.readAsJson(usagesFile, wrapper -> {
      loadCommandEntriesFrom(wrapper.getSource());
    });
  }

  private void loadCommandEntriesFrom(JsonObject obj) {
    LoadedCommandEntry.MAP_CODEC.parse(JsonOps.INSTANCE, obj)
        .mapError(string -> "Failed to load extra command help info: " + string)
        .resultOrPartial(LOGGER::error)
        .ifPresent(map -> {
          for (Entry<String, LoadedCommandEntry> entry : map.entrySet()) {
            LoadedCommandEntry loaded = entry.getValue();
            String key = entry.getKey();

            if (key.equals("example")) {
              continue;
            }

            loaded.setLabel(key);

            addEntry(loaded);
          }
        });
  }

  private void loadHelpEntriesFrom(JsonWrapper json) {
    for (Entry<String, JsonElement> entry : json.entrySet()) {
      if (!entry.getValue().isJsonObject()) {
        LOGGER.error("Help entry {} is not an object", entry.getKey());
        continue;
      }

      // Ignore the example entry
      if (entry.getKey().equals("example")) {
        continue;
      }

      JsonWrapper entryJson = JsonWrapper.wrap(entry.getValue().getAsJsonObject());

      Set<String> labels = new ObjectOpenHashSet<>();
      labels.add(entry.getKey());
      labels.addAll(entryJson.getList("aliases", JsonElement::getAsString));

      ViewerAwareMessage shortText = entryJson.get("short-text", JsonUtils::readMessage);
      ViewerAwareMessage fullText = entryJson.get("full-text", JsonUtils::readMessage);

      boolean makeCommand = entryJson.getBool("make-command");

      LoadedHelpEntry helpEntry = new LoadedHelpEntry(labels, entry.getKey(), shortText, fullText);

      if (makeCommand) {
        helpEntry.setCommand(new LoadedEntryCommand(entry.getKey(), helpEntry));
        helpEntry.getCommand().register();
      }

      addEntry(helpEntry);
      loadedEntries.add(helpEntry);
    }
  }
}