package net.arcadiusmc.scripts.preprocessor;

import com.mojang.datafixers.util.Unit;
import net.arcadiusmc.scripts.Script;
import net.arcadiusmc.utils.Result;

interface PreProcessorCallback {

  Result<Unit> postProcess(Script script);
}
