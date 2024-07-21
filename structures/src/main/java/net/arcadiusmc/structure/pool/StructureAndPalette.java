package net.arcadiusmc.structure.pool;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockStructure;

public record StructureAndPalette(Holder<BlockStructure> structure, String paletteName) {

}
