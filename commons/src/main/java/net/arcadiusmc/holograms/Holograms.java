package net.arcadiusmc.holograms;

import static net.arcadiusmc.BukkitServices.load;
import static net.arcadiusmc.BukkitServices.loadOrThrow;

import java.util.function.Consumer;
import net.arcadiusmc.registry.Registry;

public final class Holograms {
  private Holograms() {}

  static HologramService service;

  public static HologramService getService() {
    return service == null
        ? (service = loadOrThrow(HologramService.class))
        : service;
  }

  public static void updateWithSource(String sourceKey) {
    ifLoaded(service -> {
      var sources = service.getSources();
      var opt = sources.getHolder(sourceKey);

      if (opt.isEmpty()) {
        return;
      }

      service.updateWithSource(opt.get());
    });
  }

  public static void ifLoaded(Consumer<HologramService> consumer) {
    if (service != null) {
      consumer.accept(service);
      return;
    }

    load(HologramService.class).ifPresent(hologramService -> {
      service = hologramService;
      consumer.accept(service);
    });
  }

  public static void setService(HologramService service) {
    Holograms.service = service;
  }

  public static Registry<LeaderboardSource> getSources() {
    return getService().getSources();
  }

  public static void registerSource(String key, LeaderboardSource source) {
    getSources().register(key, source);
  }
}
