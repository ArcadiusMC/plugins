package net.arcadiusmc.ui.resource;

import java.util.List;
import java.util.Map;
import net.arcadiusmc.ui.struct.BodyElement;
import net.arcadiusmc.ui.style.Stylesheet;

public record ParsedDocument(
    BodyElement body,
    float width,
    float height,
    Map<String, String> options,
    List<Stylesheet> stylesheets
) {

}
