package net.arcadiusmc.ui;

import org.joml.Vector2f;
import org.joml.Vector3f;

public record TargetPage(PageView view, Vector2f screenPos, Vector3f hitPosition) {
}
