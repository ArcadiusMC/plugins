package net.arcadiusmc.items;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.math.Vectors;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

public class NonNatural {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Path directory;
  private final Map<String, WorldData> worlds = new Object2ObjectOpenHashMap<>();

  public NonNatural(Path directory) {
    this.directory = directory;
    PathUtil.ensureDirectoryExists(directory);
  }

  public void save() {
    for (WorldData value : worlds.values()) {
      value.save();
    }
  }

  public void reload() {
    // This is only ever called by staff,
    // so I see no reason to actually attempt
    // re-loading every region in memory
    for (WorldData value : worlds.values()) {
      value.clear();
    }

    worlds.clear();
  }

  private WorldData getData(World world) {
    String name = world.getName();
    WorldData data = worlds.get(name);

    if (data == null) {
      data = new WorldData(directory.resolve(name));
      worlds.put(name, data);
    }

    return data;
  }

  public void setNonNatural(World world, int x, int y, int z) {
    WorldData data = getData(world);
    data.setNonNatural(x, y, z);
  }

  public boolean isNatural(World world, int x, int y, int z) {
    WorldData data = getData(world);
    return data.isNatural(x, y, z);
  }

  record RegionVec2i(int x, int z) {

    static RegionVec2i of(int blockX, int blockZ) {
      return new RegionVec2i(blockX >> 7, blockZ >> 7);
    }
  }

  static class WorldData {
    final Map<RegionVec2i, WorldSection> loadedSections = new Object2ObjectOpenHashMap<>();
    final Path worldDirectory;

    public WorldData(Path worldDirectory) {
      this.worldDirectory = worldDirectory;
    }

    public void save() {
      for (Entry<RegionVec2i, WorldSection> e : loadedSections.entrySet()) {
        try {
          saveSection(e.getKey(), e.getValue());
        } catch (IOException exc) {
          LOGGER.error("Failed to serialize section: '{}'", e.getKey(), exc);
        }
      }
    }

    Path sectionFile(RegionVec2i pos) {
      return worldDirectory.resolve(pos.x + "." + pos.z + ".bin");
    }

    private long toLong(int x, int y, int z) {
      return Vectors.toLong(x, y, z);
    }

    public void setNonNatural(int x, int y, int z) {
      RegionVec2i regionPos = RegionVec2i.of(x, z);
      WorldSection region = getOrCreate(regionPos);

      // Only null if there was an error loading or creating the file
      if (region == null) {
        return;
      }

      long blockPos = toLong(x, y, z);
      region.set.add(blockPos);
      region.dirty = true;

      // Block was broken, player is active, don't unload
      // section just yet
      pushbackTask(regionPos, region);
    }

    public boolean isNatural(int x, int y, int z) {
      RegionVec2i regionPos = RegionVec2i.of(x, z);
      Either<WorldSection, Boolean> regionQuery = get(regionPos);

      // Right being present means either error or no region
      // If region failed to load, this will return false
      // otherwise true, as that means no region file exists,
      // meaning no blocks have been broken
      if (regionQuery.right().isPresent()) {
        return regionQuery.right().get();
      }

      WorldSection region = regionQuery.left().get();
      long blockPos = toLong(x, y, z);

      // Pushback here because these calls should only
      // be made via event listener, thus if the
      // method is called now, it's likely it'll be
      // called again later, so pushback the expiry now
      pushbackTask(regionPos, region);
      return !region.set.contains(blockPos);
    }

    public void clear() {
      for (var r : loadedSections.values()) {
        Tasks.cancel(r.unloadTask);
      }

      loadedSections.clear();
    }

    public void reset() {
      clear();

      PathUtil.safeDelete(worldDirectory, true, true)
          .resultOrPartial(LOGGER::error)
          .ifPresent(integer -> LOGGER.info("Purged {} section files", integer));

      PathUtil.ensureDirectoryExists(worldDirectory);
    }

    private Either<WorldSection, Boolean> get(RegionVec2i pos) {
      WorldSection loaded = loadedSections.get(pos);

      if (loaded != null) {
        return Either.left(loaded);
      }

      if (!Files.exists(sectionFile(pos))) {
        return Either.right(true);
      }

      try {
        WorldSection region = new WorldSection();
        loadSection(pos, region);

        loadedSections.put(pos, region);
        return Either.left(region);
      } catch (IOException e) {
        LOGGER.error("Couldn't read section: '" + pos + "'", e);
        return Either.right(false);
      }
    }

    private WorldSection getOrCreate(RegionVec2i pos) {
      WorldSection loaded = loadedSections.get(pos);

      if (loaded != null) {
        return loaded;
      }

      try {
        WorldSection region = new WorldSection();

        if (Files.exists(sectionFile(pos))) {
          loadSection(pos, region);
        } else {
          region.set = new LongOpenHashSet(200);
        }

        loadedSections.put(pos, region);
        return region;
      } catch (IOException e) {
        LOGGER.error("Couldn't load world region: '{}'", pos, e);
      }

      return null;
    }

    private void pushbackTask(RegionVec2i pos, WorldSection region) {
      Tasks.cancel(region.unloadTask);
      region.unloadTask = Tasks.runLaterAsync(() -> unloadSection(pos), Duration.ofMinutes(10));
    }

    private void unloadSection(RegionVec2i pos) {
      WorldSection section = loadedSections.get(pos);

      if (section == null) {
        return;
      }

      try {
        saveSection(pos, section);
        Tasks.cancel(section.unloadTask);
        loadedSections.remove(pos);

        LOGGER.debug("Unloaded RW section {}", pos);
      } catch (IOException e) {
        LOGGER.error("Couldn't save section: '{}'", pos, e);
      }
    }

    private void saveSection(RegionVec2i pos, WorldSection region) throws IOException {
      Path sectionFile = sectionFile(pos);

      // Don't serialize empty regions and regions
      // that don't need to be serialized
      if (region.set == null || region.set.isEmpty() || !region.dirty) {
        return;
      }

      PathUtil.ensureParentExists(sectionFile);

      OutputStream output = Files.newOutputStream(sectionFile);
      DataOutputStream stream = new DataOutputStream(output);

      writeSection(stream, region);

      stream.close();
      output.close();

      region.dirty = false;

      LOGGER.debug("Saved section {}", pos);
    }

    private void loadSection(RegionVec2i pos, WorldSection region) throws IOException {
      Path sectionFile = sectionFile(pos);

      if (!Files.exists(sectionFile)) {
        throw new NoSuchFileException(sectionFile.toString());
      }

      InputStream inputStream = Files.newInputStream(sectionFile);
      DataInputStream dataInput = new DataInputStream(inputStream);

      readSection(dataInput, region);

      dataInput.close();
      inputStream.close();
    }


    // --- READING AND WRITING SECTIONS ---
    // Note:
    // Since these files only have 1 and ONLY 1 purpose,
    // that being to store data about which blocks have
    // been broken in the resource world, the file format
    // used is immensely simple, a binary format where the
    // first 4 bytes of a file is the amount of positions
    // stored within it, and the rest of the bytes are
    // simply the longs themselves
    //
    // The longs themselves are simply packed coordinates,
    // see toLong(Vector3i) for how x y z coordinates get
    // packed to a single long
    //     -- Jules <3

    private void writeSection(DataOutput output, WorldSection region) throws IOException {
      output.writeInt(region.set.size());

      for (long l : region.set) {
        output.writeLong(l);
      }
    }

    private void readSection(DataInput input, WorldSection region) throws IOException {
      int expected = input.readInt();
      region.set = new LongOpenHashSet(expected);

      for (int i = 0; i < expected; i++) {
        region.set.add(input.readLong());
      }
    }
  }

  static class WorldSection {
    LongSet set;
    boolean dirty = false;
    BukkitTask unloadTask;
  }
}
