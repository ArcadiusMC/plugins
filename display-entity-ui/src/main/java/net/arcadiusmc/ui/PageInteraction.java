package net.arcadiusmc.ui;

import org.joml.Vector2f;
import org.joml.Vector3f;

public record PageInteraction(Vector3f worldPos, Vector2f screenPos, InteractionType type) {

}
