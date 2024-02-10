package net.arcadiusmc.scripts.pack;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.arcadiusmc.registry.Holder;

class TopologicalSort {

  static List<Holder<ScriptPack>> sort(List<Holder<ScriptPack>> packs) {
    SortState state = new SortState(packs.size());

    for (Holder<ScriptPack> pack : packs) {
      state.packLookup.put(pack.getKey(), pack);
    }

    for (Holder<ScriptPack> pack : packs) {
      if (state.visited.contains(pack.getKey())) {
        continue;
      }

      dfs(pack, state);
    }

    return state.results;
  }

  private static void dfs(Holder<ScriptPack> pack, SortState state) {
    state.visited.add(pack.getKey());

    for (String requiredScript : pack.getValue().getMeta().getRequiredScripts()) {
      if (state.visited.contains(requiredScript)) {
        continue;
      }

      Holder<ScriptPack> dependent = state.packLookup.get(requiredScript);

      if (dependent == null) {
        continue;
      }

      dfs(dependent, state);
    }

    state.results.add(pack);
  }

  private record SortState(
      Set<String> visited,
      Map<String, Holder<ScriptPack>> packLookup,
      List<Holder<ScriptPack>> results
  ) {

    public SortState(int size) {
      this(
          new ObjectOpenHashSet<>(size),
          new HashMap<>(size),
          new ArrayList<>(size)
      );
    }
  }
}
